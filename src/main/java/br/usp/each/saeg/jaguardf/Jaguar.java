package br.usp.each.saeg.jaguardf;

import br.usp.each.saeg.badua.core.analysis.Analyzer;
import br.usp.each.saeg.badua.core.analysis.ClassCoverage;
import br.usp.each.saeg.badua.core.analysis.MethodCoverage;
import br.usp.each.saeg.badua.core.analysis.SourceLineDefUseChain;
import br.usp.each.saeg.badua.core.data.ExecutionData;
import br.usp.each.saeg.badua.core.data.ExecutionDataStore;

import br.usp.each.saeg.jaguardf.analysis.DuaCoverageBuilder;
import br.usp.each.saeg.jaguardf.output.Matrix;
import br.usp.each.saeg.jaguardf.output.Spectra;
import br.usp.each.saeg.jaguardf.utils.FileUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Mario Concilio
 */
public class Jaguar {

    private final static Logger logger = LoggerFactory.getLogger("JaguarDF");

    private int nTests = 0;
    private int nTestsFailed = 0;
    private long startTime;

    private Spectra spectra;
    private Matrix matrix;

    private ExecutionDataStore merge;

    private Map<String, File> classCache;

    public Jaguar(File classesDir, File projectDir) {
        this.spectra = new Spectra(projectDir);
        this.matrix = new Matrix(projectDir);
        this.startTime = System.currentTimeMillis();
        this.classCache = getAllClasses(classesDir);
    }

    public void collect(final ExecutionDataStore executionDataStore, boolean currentTestFailed) {
        logger.debug("Test # {}", nTests);

        if (this.merge == null) {
            this.merge = executionDataStore;
        }
        else {
            executionDataStore.accept(this.merge);
        }

        long startTime = System.currentTimeMillis();
        DuaCoverageBuilder duaCoverageBuilder = new DuaCoverageBuilder();
        Analyzer analyzer = new Analyzer(executionDataStore, duaCoverageBuilder);
        analyzeCoveredClasses(executionDataStore, analyzer);
        logger.debug("Time to analyze DF data: {}", System.currentTimeMillis() - startTime);

        startTime = System.currentTimeMillis();

        try {
            collectDuaCoverage(currentTestFailed, duaCoverageBuilder);
        }
        catch (IOException e) {
            logger.error("Error when collecting dua coverage", e);
        }

        logger.debug(
                "Time to read and store data: {} , from {} classes",
                System.currentTimeMillis() - startTime,
                duaCoverageBuilder.getClasses().size()
        );
    }

    public long getTotalTime() {
        return System.currentTimeMillis() - startTime;
    }

    public int getTests() {
        return nTests;
    }

    public int getTestsFailed() {
        return nTestsFailed;
    }

    public int increaseTests() {
        return ++nTests;
    }

    public int increaseTestsFailed() {
        return ++nTestsFailed;
    }

    /*
     * Helper methods
     */

    private Map<String, File> getAllClasses(File dir) {
        Path dirPath = dir.toPath();
        Map<String, File> classCache;

        try (Stream<Path> stream = Files.walk(dirPath)) {
            classCache = stream
                    .filter(FileUtils::isJavaClass)
                    .collect(Collectors.toMap(path -> FileUtils.toClassName(dirPath, path), Path::toFile));
        }
        catch (IOException e) {
            classCache = new HashMap<>();
            logger.error("Error when retrieving classes from project", e);
        }

        logger.trace("Class cache: {}", classCache.keySet());
        logger.debug("Load {} classes from project", classCache.size());

        return classCache;
    }

    private void analyzeCoveredClasses(ExecutionDataStore executionData, Analyzer analyzer) {
        logger.trace("Analyzing covered classes");

        Collection<File> classFiles = classFilesOfStore(executionData);
        logger.trace("Class files size = {}", classFiles.size());

        for (File classFile : classFiles) {
            logger.trace("Analyzing class {}", classFile.getPath());

            try (InputStream inputStream = new FileInputStream(classFile)) {
                analyzer.analyze(inputStream, classFile.getPath());
            }
            catch (Exception e) {
                logger.warn("Exception during analysis of file " + classFile.getAbsolutePath(), e);
            }
        }
    }

    private Collection<File> classFilesOfStore(ExecutionDataStore executionDataStore) {
        Collection<File> result = new ArrayList<>();
        for (ExecutionData data : executionDataStore.getContents()) {
            String vmClassName = data.getName();
            File classFile = this.classCache.get(vmClassName);

            if (classFile != null) {
                result.add(classFile);
            }
        }

        return result;
    }

    private void collectDuaCoverage(boolean currentTestFailed, DuaCoverageBuilder coverageVisitor) throws IOException {
        int totalDuas = 0;
        int totalDuasCovered = 0;

        matrix.isTestFailed(currentTestFailed);
        matrix.remainingClassesCoverage(this.classCache.keySet(), coverageVisitor);

        for (ClassCoverage clazz : coverageVisitor.getClasses()) {
            logger.trace("Collecting duas from class: {}", clazz.getName());

            matrix.addClass(clazz);
            spectra.addClass(clazz);

            for (MethodCoverage method : clazz.getMethods()) {
                logger.trace("Collecting duas from method: {}", method.getName());

                for (SourceLineDefUseChain dua : method.getDefUses()) {
                    logger.trace("Collecting information from dua: ({},{}, {})", dua.def, dua.use, dua.var);

                    totalDuas++;
                    matrix.addDua(dua);
                    spectra.addDua(clazz, method, dua);

                    if (dua.covered) {
                        totalDuasCovered++;
                    }
                }
            }

            matrix.next();
            spectra.next();
        }

        logger.debug("#duas = {}, #coveredDuas = {}", totalDuas, totalDuasCovered);
    }
}
