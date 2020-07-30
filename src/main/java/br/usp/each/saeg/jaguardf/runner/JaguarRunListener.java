package br.usp.each.saeg.jaguardf.runner;

import br.usp.each.saeg.jaguardf.Jaguar;
import br.usp.each.saeg.jaguardf.JaguarClient;

import br.usp.each.saeg.badua.core.data.ExecutionDataStore;
import br.usp.each.saeg.badua.core.data.ExecutionDataWriter;

import org.junit.runner.notification.RunListener;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mario Concilio
 */
public class JaguarRunListener extends RunListener {

    private static Logger logger = LoggerFactory.getLogger("JaguarDF");

    private final Jaguar jaguar;
    private final JaguarClient client;
    private boolean currentTestFailed;

    public JaguarRunListener(Jaguar jaguar, JaguarClient client) {
        this.jaguar = jaguar;
        this.client = client;
    }

    @Override
    public void testStarted(Description description) throws Exception {
        currentTestFailed = false;
        jaguar.increaseTests();
    }

    @Override
    public void testFailure(Failure failure) throws Exception {
        currentTestFailed = true;
        jaguar.increaseTestsFailed();
    }

    @Override
    public void testFinished(Description description) {
        printTestResult(description);

        long startTime = System.currentTimeMillis();
        ExecutionDataStore dataStore = client.read();
        logger.debug("Time to receive data: {}", System.currentTimeMillis() - startTime);

        startTime = System.currentTimeMillis();
        jaguar.collect(dataStore, currentTestFailed);
        logger.debug("Time to collect data: {}", System.currentTimeMillis() - startTime);
    }

    @Override
    public void testRunFinished(Result result) {
        logger.debug("Time to run JaguarDF: {}", jaguar.getTotalTime());
    }

    private void printTestResult(Description description) {
        if (currentTestFailed) {
            logger.info("Test {} : Failed", description.getDisplayName());
        }
        else {
            logger.debug("Test {} : Passed", description.getDisplayName());
        }
    }
}
