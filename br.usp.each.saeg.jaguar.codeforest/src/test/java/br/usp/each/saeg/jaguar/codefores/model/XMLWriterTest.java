package br.usp.each.saeg.jaguar.codefores.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXB;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import br.usp.each.saeg.jaguar.codeforest.model.Class;
import br.usp.each.saeg.jaguar.codeforest.model.HierarchicalFaultClassification;
import br.usp.each.saeg.jaguar.codeforest.model.LineRequirement;
import br.usp.each.saeg.jaguar.codeforest.model.Method;
import br.usp.each.saeg.jaguar.codeforest.model.Package;
import br.usp.each.saeg.jaguar.codeforest.model.Requirement;
import br.usp.each.saeg.jaguar.codeforest.model.Requirement.Type;

public class XMLWriterTest {

	private static final String CODE_FOREST_XML_FILE = "./src/test/resources/codeForestTest.xml";
	private static HierarchicalFaultClassification faultClassification;

	@BeforeClass
	public static void beforeClass(){
		faultClassification = createFaultClassificationObject();
	}

	@Test
	public void writeCodeForest() {
		final File codeForestXML = new File(CODE_FOREST_XML_FILE);
		JAXB.marshal(faultClassification, codeForestXML);
		final HierarchicalFaultClassification faultClassificationSaved = JAXB.unmarshal(new File(
				CODE_FOREST_XML_FILE), HierarchicalFaultClassification.class);
		Assert.assertEquals(faultClassification, faultClassificationSaved);
	}

	private static HierarchicalFaultClassification createFaultClassificationObject() {
		final LineRequirement requirement1 = new LineRequirement();
		requirement1.setLocation(61);
		requirement1.setName("0");
		requirement1.setSuspiciousValue(0.0);

		final LineRequirement requirement2 = new LineRequirement();
		requirement2.setLocation(100);
		requirement2.setName("108");
		requirement2.setSuspiciousValue(0.0);

		final List<Requirement> requirementList = new ArrayList<Requirement>();
		requirementList.add(requirement1);
		requirementList.add(requirement2);

		final Method method1 = new Method();
		method1.setId(1);
		method1.setLocation(60);
		method1.setName("inverseCumulativeProbability(double)");
		method1.setNumber(11);
		method1.setPosition(115);
		method1.setSuspiciousValue(0.0);
		method1.setRequirements(requirementList);

		final Method method2 = new Method();
		method2.setId(0);
		method2.setLocation(44);
		method2.setName("AbstractContinuousDistribution()");
		method2.setNumber(2);
		method2.setPosition(111);
		method2.setSuspiciousValue(0.0);

		final List<Method> methodList = new ArrayList<Method>();
		methodList.add(method1);
		methodList.add(method2);

		final Class class1 = new Class();
		class1.setLocation(19);
		class1.setName("org.apache.commons.math.distribution.AbstractContinuousDistribution");
		class1.setNumber(13);
		class1.setSuspiciousValue(0.0);
		class1.setMethods(methodList);

		final Class class2 = new Class();
		class2.setLocation(10);
		class2.setName("org.apache.commons.math.distribution.AbstractContinuousDistribution$1");
		class2.setNumber(5);
		class2.setSuspiciousValue(0.0);

		final List<Class> classList = new ArrayList<Class>();
		classList.add(class1);
		classList.add(class2);

		final Package package1 = new Package();
		package1.setName("org.apache.commons.math.distribution");
		package1.setNumber(359);
		package1.setSuspiciousValue(0.0);
		package1.setClasses(classList);

		final Package package2 = new Package();
		package2.setName("org.apache.commons.math.stat.inference");
		package2.setNumber(293);
		package2.setSuspiciousValue(0.0);


		final List<Package> packageSet = new ArrayList<Package>();
		packageSet.add(package1);
		packageSet.add(package2);


		final HierarchicalFaultClassification xmlObject = new HierarchicalFaultClassification();
		xmlObject.setProject("fault localization");
		xmlObject.setHeuristic("TARANTULA");
		xmlObject.setRequirementType(Type.LINE);
		xmlObject.setPackages(packageSet);
		return xmlObject;
	}
}
