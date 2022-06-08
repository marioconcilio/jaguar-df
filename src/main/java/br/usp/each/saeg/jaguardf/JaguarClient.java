package br.usp.each.saeg.jaguardf;

import java.io.IOException;
import java.util.Arrays;

import br.usp.each.saeg.badua.agent.rt.internal_60983f5.Agent;
import br.usp.each.saeg.badua.core.data.ExecutionDataStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mario Concilio
 */
public class JaguarClient {

    private final static Logger logger = LoggerFactory.getLogger("JaguarDF");

    public ExecutionDataStore read() {
        Agent agent = Agent.getInstance();
        ExecutionDataStore current = new ExecutionDataStore();

        agent.getData().collect(data ->
                current.visitClassExecution(new br.usp.each.saeg.badua.core.data.ExecutionData(
                        data.getId(),
                        data.getName(),
                        Arrays.copyOf(data.getData(), data.getData().length))
                )
        );

        try {
            agent.dump(true);
        }
        catch (IOException e) {
            logger.error("Error on agent dump", e);
        }

        return current;
    }
}
