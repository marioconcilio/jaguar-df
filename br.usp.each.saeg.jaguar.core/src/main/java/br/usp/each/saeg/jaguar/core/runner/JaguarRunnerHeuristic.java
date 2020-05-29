package br.usp.each.saeg.jaguar.core.runner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import br.usp.each.saeg.jaguar.core.heuristic.Heuristic;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface JaguarRunnerHeuristic {
	/**
	 * @return the heuristic to be used
	 */
	public Class<? extends Heuristic> value();
	public boolean isDataflow() default false;
}
