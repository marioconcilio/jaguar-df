package br.usp.each.saeg.jaguardf.cli;

import java.io.File;
import java.nio.file.Paths;

import org.kohsuke.args4j.Option;

/**
 * @author Mario Concilio
 */
public class JaguarRunnerOptions {

    private File classesDir;
    private File testsDir;

    @Option(name = "--help",
            help = true,
            usage = "show this help message")
    private Boolean help = false;

    @Option(name = "--projectDir",
            aliases = {"-p"},
            required = true,
            usage = "relative path where project is located")
    private File projectDir = new File("");

    @Option(name = "--testSuite",
            aliases = {"-s"},
            usage = "the test suite to run\n" +
                    "if a test suite is specified, Jaguar runs it instead of all classes in testDir")
    private String testSuite = "";

    @Option(name = "--tests",
            aliases = {"-u"},
            required = true,
            usage = "file containing all tests to run")
    private File tests = new File("");

    @Option(name = "--logLevel",
            aliases = {"-l"},
            usage = "the log level\n ALL, TRACE, DEBUG, INFO, WARN, ERROR")
    private String logLevel = "INFO";

    public JaguarRunnerOptions() {
        classesDir = Paths.get(projectDir.getAbsolutePath(), "target", "classes").toFile();
        testsDir = Paths.get(projectDir.getAbsolutePath(), "target", "test-classes").toFile();
    }

    /*
     * Setters
     */

    @Option(name = "--classesDir",
            aliases = {"-c"},
            usage = "relative path where compiled classes are located")
    public void setClassesDir(String path) {
        this.classesDir = new File(projectDir, path);
    }

    @Option(name = "--testsDir",
            aliases = {"-t"},
            usage = "relative path where compiled tests are located")
    public void setTestsDir(String path) {
        this.testsDir = new File(projectDir, path);
    }

    /*
     * Getters
     */

    public Boolean isHelp() {
        return help;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public File getProjectDir() {
        return projectDir;
    }

    public File getClassesDir() {
        return classesDir;
    }

    public File getTestsDir() {
        return testsDir;
    }

    public File getTests() {
        return tests;
    }

    public String getTestSuite() {
        return testSuite;
    }

    @Override
    public String toString() {
        return "JaguarRunnerOptions \n"
                + "help = " + help + "\n"
                + "projectDir = " + projectDir.getPath() + "\n"
                + "classesDir = " + classesDir.getPath() + "\n"
                + "testsDir = " + testsDir.getPath() + "\n"
                + "testSuite = " + testSuite + "\n"
                + "tests = " + tests.getPath() + "\n"
                + "logLevel = " + logLevel;
    }
}
