package br.usp.each.saeg.jaguardf.cli;

import br.usp.each.saeg.jaguardf.Jaguar;
import br.usp.each.saeg.jaguardf.JaguarClient;
import br.usp.each.saeg.jaguardf.runner.JaguarRunListener;
import br.usp.each.saeg.jaguardf.utils.FileUtils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.LoggerFactory;
import org.junit.runner.JUnitCore;

import java.io.File;
import java.util.Arrays;

/**
 * @author Mario Concilio
 */
public class JaguarRunner {

    private static Logger logger = (Logger) LoggerFactory.getLogger("JaguarDF");

    private final JUnitCore junit = new JUnitCore();

    private final File classesDir;
    private final File projectDir;
    private final File testsFile;
    private final String testSuite;

    public JaguarRunner(File classesDir, File projectDir, File testsFile, String testSuite) {
        this.classesDir = classesDir;
        this.projectDir = projectDir;
        this.testsFile = testsFile;
        this.testSuite = testSuite;
    }

    private void run() {
        Class<?> suiteClass = null;
        if (testSuite != null && !testSuite.isEmpty()) {
            try {
                suiteClass = Class.forName(testSuite);
            }
            catch (ClassNotFoundException e) {
                logger.warn("Test suite class {} not found. Running all tests in test folder", testSuite);
            }
        }

        Class<?>[] classes;
        if (suiteClass == null) {
            classes = FileUtils.classesFromListFile(testsFile);
        }
        else {
            logger.debug("Running test suite = {}", suiteClass);
            classes = new Class<?>[] { suiteClass };
        }

        final Jaguar jaguar = new Jaguar(classesDir, projectDir);
        final JaguarClient client = new JaguarClient();

        junit.addListener(new JaguarRunListener(jaguar, client));
        junit.run(classes);
    }

    public static void main(String[] args) {
        logger.info("Welcome to Jaguar-DF CLI!");

        final JaguarRunnerOptions options = new JaguarRunnerOptions();
        final CmdLineParser parser = new CmdLineParser(options);

        try {
            logger.debug("CLI Arguments: {}", Arrays.toString(args));
            parser.parseArgument(args);
        }
        catch (CmdLineException e) {
            logger.error(e.getLocalizedMessage());
            parser.printUsage(System.err);
            System.exit(1);
        }

        if (options.isHelp()){
            parser.printUsage(System.err);
            System.exit(0);
        }

        setLogLevel(options.getLogLevel());
        logger.debug(options.toString());

        new JaguarRunner(
                options.getClassesDir(),
                options.getProjectDir(),
                options.getTests(),
                options.getTestSuite()
        ).run();

        logger.info("Jaguar-DF has finished!");
        System.exit(0);
    }

    private static void setLogLevel(String logLevel) {
        Level level;

        switch (logLevel.toUpperCase()) {
            case "ALL":
                level = Level.ALL;
                break;

            case "TRACE":
                level = Level.TRACE;
                break;

            case "DEBUG":
                level = Level.DEBUG;
                break;

            case "INFO":
                level = Level.INFO;
                break;

            case "WARN":
                level = Level.WARN;
                break;

            case "ERROR":
                level = Level.ERROR;
                break;

            default:
                level = Level.OFF;
        }

        logger.setLevel(level);
    }
}
