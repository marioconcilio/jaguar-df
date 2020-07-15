package br.usp.each.saeg.jaguar.core.runner;

import br.usp.each.saeg.badua.core.data.ExecutionDataStore;
import br.usp.each.saeg.badua.core.data.ExecutionDataWriter;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.usp.each.saeg.jaguar.core.JaguarClient;
import br.usp.each.saeg.jaguar.core.Jaguar;

import java.io.FileOutputStream;

public class JaguarRunListener extends RunListener {

	private static Logger logger = LoggerFactory.getLogger("JaguarLogger");
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
		jaguar.increaseNTests();
	}

	@Override
	public void testFailure(Failure failure) throws Exception {
		currentTestFailed = true;
		jaguar.increaseNTestsFailed();
	}

	@Override
	public void testFinished(Description description) {
		printTestResult(description);
 		try {
 			long startTime = System.currentTimeMillis();
 			ExecutionDataStore dataStore = client.read();
 			logger.debug("Time to receive data: {}", System.currentTimeMillis() - startTime);
 			
 			startTime = System.currentTimeMillis();
			jaguar.collect(dataStore, currentTestFailed, description.getDisplayName());
			logger.debug("Time to collect data: {}", System.currentTimeMillis() - startTime);

//			final FileOutputStream output = new FileOutputStream("jaguar_" + description.getDisplayName() + ".out");
//			jaguar.merge.accept(new ExecutionDataWriter(output));
		} catch (Exception e) {
			logger.error("Exception: " + e.toString());
			logger.error("Exception Message : " + e.getMessage());
			logger.error("Stacktrace: ");
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}

	@Override
	public void testRunFinished(Result result) throws Exception {
		final FileOutputStream output = new FileOutputStream("jaguar.out");
		try {
			jaguar.merge.accept(new ExecutionDataWriter(output));
		}
		finally {
			output.close();
		}
	}

	private void printTestResult(Description description) {
		if (currentTestFailed){
			logger.info("Test {} : Failed", description.getDisplayName());
		}else{
			logger.debug("Test {} : Passed", description.getDisplayName());
		}
	}

}
