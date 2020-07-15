package br.usp.each.saeg.jaguar.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import br.usp.each.saeg.badua.core.analysis.Analyzer;
import br.usp.each.saeg.badua.core.analysis.ClassCoverage;
import br.usp.each.saeg.badua.core.analysis.MethodCoverage;
import br.usp.each.saeg.badua.core.analysis.SourceLineDefUseChain;
import br.usp.each.saeg.badua.core.data.ExecutionData;
import br.usp.each.saeg.badua.core.data.ExecutionDataStore;

import br.usp.each.saeg.jaguar.core.output.Spectra;
import org.apache.commons.lang3.StringUtils;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ILine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.usp.each.saeg.jaguar.core.heuristic.Heuristic;
import br.usp.each.saeg.jaguar.core.heuristic.HeuristicCalculator;
import br.usp.each.saeg.jaguar.core.model.core.CoverageStatus;
import br.usp.each.saeg.jaguar.core.model.core.requirement.AbstractTestRequirement;
import br.usp.each.saeg.jaguar.core.analysis.DuaCoverageBuilder;
import br.usp.each.saeg.jaguar.core.output.Matrix;

/**
 * This class store the coverage information received from Jacoco and generate a
 * suspicious test requirement rank based on one SFL Heuristic.
 * 
 * @author Henrique Ribeiro
 */
public class Jaguar {

	private final static Logger logger = LoggerFactory.getLogger("JaguarLogger");

	private static final String XML_NAME = "jaguar_output";
	private int nTests = 0;
	private int nTestsFailed = 0;
	private Map<String, File> classFilesCache;

	private JaguarSFL sfl = new JaguarSFL();
	private Spectra spectra;
	private Matrix matrix;
	
	private Long startTime;
	private Long totalTimeSpent;

	public ExecutionDataStore merge;

	/**
	 * Construct the Jaguar object.
	 *
	 * @param classesDir
	 *            the compiled classes dir to be instrumented
	 */
	public Jaguar(File classesDir) {
		this(classesDir, null);
	}

	public Jaguar(File classesDir, File projectDir) {
		this.spectra = new Spectra(projectDir);
		this.matrix = new Matrix(projectDir);

		this.startTime = System.currentTimeMillis();

		classFilesCache = new HashMap<>();
		populateClassFilesCache(classesDir, "");
		logger.debug("ClassFilesCache size = {}", classFilesCache.size());
	}

	private void populateClassFilesCache(File dir, String path) {
		File[] files = dir.listFiles();
		if (files == null) {
			return;
		}
		for (File file : files) {
			if (file.isDirectory()) {
				populateClassFilesCache(file, path + file.getName() + "/");
			} else if (file.getName().endsWith(".class")) {
				String className = path + StringUtils.removeEnd(file.getName(), ".class");
				classFilesCache.put(className, file);
				logger.trace("Added {} to classFilesCache", className);
			}
		}
	}

	/**
	 * Receive the coverage information and store it on Test Requiremtns.
	 * 
	 * @param executionData
	 *            the covarege data from Jacoco
	 * @param currentTestFailed
	 *            result of the test
	 * @throws IOException
	 * 
	 */
	public void collect(final ExecutionDataStore executionData, boolean currentTestFailed, String testName) throws IOException {
		logger.debug("Test # {}", nTests);
//		if (executionData instanceof DataFlowExecutionDataStore) {
			logger.trace("Collecting DF coverage");

			if (this.merge == null) {
				this.merge = executionData;
			}
			else {
				executionData.accept(this.merge);
			}

			long startTime = System.currentTimeMillis();
			DuaCoverageBuilder duaCoverageBuilder = new DuaCoverageBuilder();
			//AbstractAnalyzer analyzer = new DataflowAnalyzer(executionData, duaCoverageBuilder);
			Analyzer analyzer = new Analyzer(executionData, duaCoverageBuilder);
			analyzeCoveredClasses(executionData, analyzer);
			logger.debug("Time to analyze DF data: {}", System.currentTimeMillis() - startTime);

			startTime = System.currentTimeMillis();
			collectDuaCoverage(currentTestFailed, duaCoverageBuilder, testName);
			logger.debug("Time to read and store data: {} , from {} classes", System.currentTimeMillis() - startTime, duaCoverageBuilder
					.getClasses().size());

//		}
//		else if (executionData instanceof ControlFlowExecutionDataStore) {
//			logger.trace("Collecting CF coverage");
//
//			long startTime = System.currentTimeMillis();
//			CoverageBuilder coverageBuilder = new CoverageBuilder();
//			AbstractAnalyzer analyzer = new ControlFlowAnalyzer(executionData, coverageBuilder);
//			analyzeCoveredClasses(executionData, analyzer);
//			logger.debug("Time to analyze CF data: {}", System.currentTimeMillis() - startTime);
//
//			startTime = System.currentTimeMillis();
//			collectLineCoverage(currentTestFailed, coverageBuilder);
//			logger.debug("Time to read and store data: {} , from {} classes", System.currentTimeMillis() - startTime, coverageBuilder
//					.getClasses().size());
//
//		}
//		else {
//			logger.error("Unknown DataStore - {}", executionData.getClass().getName());
//		}

	}

	private void analyzeCoveredClasses(ExecutionDataStore executionData, Analyzer analyzer) {
		logger.trace("Analyzing covered classes");



		Collection<File> classFiles = classFilesOfStore(executionData);
		logger.trace("Class files size = {}", classFiles.size());

		for (File classFile : classFiles) {
			logger.trace("Analyzing class {}", classFile.getPath());

			try (InputStream inputStream = new FileInputStream(classFile)) {
				analyzer.analyze(inputStream, classFile.getPath());
			} catch (Exception e) {
				logger.warn("Exception during analysis of file " + classFile.getAbsolutePath(), e);
			}
		}
	}

	private Collection<File> classFilesOfStore(ExecutionDataStore executionDataStore) {
		Collection<File> result = new ArrayList<>();
		for (ExecutionData data : executionDataStore.getContents()) {
			String vmClassName = data.getName();
			File classFile = classFilesCache.get(vmClassName);
			if (classFile != null) {
				result.add(classFile);
			}
		}
		return result;
	}

	private void collectDuaCoverage(boolean currentTestFailed, DuaCoverageBuilder coverageVisitor, String testName) throws IOException {
		int totalDuas = 0;
		int totalDuasCovered = 0;

		matrix.isTestFailed(currentTestFailed);
		matrix.remainingClassesCoverage(this.classFilesCache.keySet(), coverageVisitor);

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
						sfl.updateRequirement(clazz, method, dua, currentTestFailed);
					}
				}
			}

			matrix.next();
			spectra.next();
		}

		logger.debug("#duas = {}, #coveredDuas = {}", totalDuas, totalDuasCovered);
	}

	private void collectLineCoverage(boolean currentTestFailed, CoverageBuilder coverageVisitor) {
		int totalLines = 0;
		int totalLinesCovered = 0;
		for (IClassCoverage clazz : coverageVisitor.getClasses()) {
			logger.trace("Collecting lines from class " + clazz.getName());
			CoverageStatus coverageStatus = CoverageStatus.as(clazz.getClassCounter().getStatus());
			if (CoverageStatus.FULLY_COVERED == coverageStatus || CoverageStatus.PARTLY_COVERED == coverageStatus) {
				int firstLine = clazz.getFirstLine();
				int lastLine = clazz.getLastLine();
				if (firstLine >= 0) {
					for (int currentLine = firstLine; currentLine <= lastLine; currentLine++) {
						totalLines++;
						ILine line = clazz.getLine(currentLine);
						logger.trace("Collecting information from line {}", currentLine);
						coverageStatus = CoverageStatus.as(line.getStatus());
						if (CoverageStatus.FULLY_COVERED == coverageStatus || CoverageStatus.PARTLY_COVERED == coverageStatus) {
							totalLinesCovered++;
							sfl.updateRequirement(clazz, currentLine, currentTestFailed);
						}
					}
				}
			}
		}
		logger.debug("#lines = {}, #coveredlines = {}", totalLines, totalLinesCovered);

	}

	/**
	 * Calculate the rank based on the heuristic and testRequirements. Return
	 * the rank in descending order.
	 * 
	 * @return the rank in descending order.
	 * 
	 */
	public ArrayList<AbstractTestRequirement> generateRank(Heuristic heuristic) {
		logger.debug("Rank calculation started...");
		HeuristicCalculator calc = new HeuristicCalculator(heuristic, sfl.getTestRequirements().values(), nTests - nTestsFailed, nTestsFailed);
		ArrayList<AbstractTestRequirement> result = calc.calculateRank();
		logger.debug("Rank calculation finished.");
		return result;
	}

	/**
	 * Use the given testRequirements to generate the Flat output XML. Using the
	 * default name.
	 *
	 * @param heuristic
	 *            the heuristic to be used on the fault localization rank.
	 * @param projectDir
	 *            the directory in which the output folder and files will be
	 *            written
	 *
	 */
	public void generateFlatXML(Heuristic heuristic, File projectDir) {
		generateFlatXML(heuristic, projectDir, XML_NAME);
	}

	/**
	 * Use the given testRequirements to generate the Flat output XML, using the
	 * parameter fileName.
	 *
	 * @param heuristic
	 *            the heuristic to be used on the fault localization rank.
	 * @param projectDir
	 *            the directory in which the output folder and files will be
	 *            written
	 * @param fileName
	 *            the name of the output xml file
	 *
	 */
	public void generateFlatXML(Heuristic heuristic, File projectDir, String fileName) {
//		ArrayList<AbstractTestRequirement> testRequirements = generateRank(heuristic);
//		FlatXmlWriter xmlWriter = new FlatXmlWriter(testRequirements, heuristic, totalTimeSpent);
//		xmlWriter.generateXML(projectDir, fileName);
	}

	/**
	 * Use the given testRequirements to generate the Hierarchical output XML.
	 * Using the default name.
	 *
	 * @param heuristic
	 *            the heuristic to be used on the fault localization rank.
	 * @param projectDir
	 *            the directory in which the output folder and files will be
	 *            written
	 *
	 */
	public void generateHierarchicalXML(Heuristic heuristic, File projectDir) {
		generateHierarchicalXML(heuristic, projectDir, XML_NAME);
	}

	/**
	 * Use the given testRequirements to generate the Hierarchical output XML,
	 * using the parameter fileName.
	 *
	 * @param heuristic
	 *            the heuristic used
	 * @param projectDir
	 *            the directory in which the output folder and files will be
	 *            written
	 * @param fileName
	 *            the name of the output xml file
	 *
	 */
	public void generateHierarchicalXML(Heuristic heuristic, File projectDir, String fileName) {
//		ArrayList<AbstractTestRequirement> testRequirements = generateRank(heuristic);
//		HierarchicalXmlWriter xmlWriter = new HierarchicalXmlWriter(testRequirements, heuristic, totalTimeSpent);
//		xmlWriter.generateXML(projectDir, fileName);
	}

	/**
	 * Currently only used to save the total time spent since Jaguar was
	 * created.
	 * 
	 */
	public void finish() {
		totalTimeSpent = System.currentTimeMillis() - startTime;
	}

	public int getnTests() {
		return nTests;
	}

	public int getnTestsFailed() {
		return nTestsFailed;
	}

	public int increaseNTests() {
		return ++nTests;
	}

	public int increaseNTestsFailed() {
		return ++nTestsFailed;
	}

}
