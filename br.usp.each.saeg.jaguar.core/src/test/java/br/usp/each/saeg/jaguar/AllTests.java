package br.usp.each.saeg.jaguar;

import org.junit.runner.RunWith;

import br.usp.each.saeg.jaguar.core.heuristic.OchiaiHeuristic;
import br.usp.each.saeg.jaguar.core.runner.JaguarRunnerHeuristic;
import br.usp.each.saeg.jaguar.core.runner.JaguarSuite;

@RunWith(JaguarSuite.class)
@JaguarRunnerHeuristic(value=OchiaiHeuristic.class, isDataflow=true)
public class AllTests {

}