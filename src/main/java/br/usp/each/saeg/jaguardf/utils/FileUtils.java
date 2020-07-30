package br.usp.each.saeg.jaguardf.utils;

import ch.qos.logback.classic.Logger;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Mario Concilio
 */
public class FileUtils {

    private static Logger logger = (Logger) LoggerFactory.getLogger("JaguarDF");

    public static Class<?>[] classesFromListFile(File listFile) {
        Class<?>[] classes = null;

        try (BufferedReader br = Files.newBufferedReader(listFile.toPath())) {
            classes = br.lines()
                    .map(FileUtils::classForName)
                    .toArray(Class<?>[]::new);
        }
        catch (IOException e) {
            logger.error("Error when reading classes from file", e);
            e.printStackTrace();
        }

        return classes;
    }

    public static Class<?> classForName(String className) {
        Class<?> clazz = null;

        try {
            clazz = Class.forName(className);
        }
        catch (ClassNotFoundException e) {
            logger.warn("Could not load class: " + className, e);
        }

        return clazz;
    }

    public static boolean isJavaClass(Path filename) {
        return Files.isRegularFile(filename) &&
                "class".equals(FilenameUtils.getExtension(filename.toString()));
    }

    public static String toClassName(Path dir, Path filename) {
        return FilenameUtils.removeExtension(dir.relativize(filename).toString());
    }
}
