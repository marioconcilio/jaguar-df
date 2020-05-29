package br.usp.each.saeg.jaguar.core.output;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * @author Mario Concilio
 */
public class CoverageFile {

    private static final Logger logger = LoggerFactory.getLogger("JaguarLogger");
    private static final String JAGUAR_DIR = ".jaguar";

    private String folderName;
    protected File path;

    public CoverageFile(File path, String folderName) {
        this.folderName = folderName;
        this.path = new File(path, JAGUAR_DIR + File.separator + folderName);
        if (this.path.exists()) {
            deletePath();
        }

        this.path.mkdirs();
    }

    protected static String classFullName(String className) {
        return className.replaceAll("/", ".");
    }

    protected String fileNameForClass(String className) {
        final StringBuilder str = new StringBuilder(classFullName(className))
                .append('.')
                .append(folderName);

        return str.toString();
    }

    protected BufferedWriter createWriter(String fileName) throws IOException {
        File coverageFile = new File(path, fileName);
        FileWriter fileWriter = new FileWriter(coverageFile, true);

        return new BufferedWriter(fileWriter);
    }

    private void deletePath() {
        try {
            Files.walk(this.path.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
        catch (IOException e) {
            logger.warn("Could not delete {}", this.path.getPath());
        }
    }
}
