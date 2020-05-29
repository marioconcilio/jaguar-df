package br.usp.each.saeg.jaguar.plugin;

/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     David Saff (saff@mit.edu) - bug 102632: [JUnit] Support for JUnit 4.
 *     Robert Konigsberg <konigsberg@google.com> - [JUnit] Leverage AbstractJavaLaunchConfigurationDelegate.getMainTypeName in JUnitLaunchConfigurationDelegate - https://bugs.eclipse.org/bugs/show_bug.cgi?id=280114
 *     Achim Demelt <a.demelt@exxcellent.de> - [junit] Separate UI from non-UI code - https://bugs.eclipse.org/bugs/show_bug.cgi?id=278844
 *******************************************************************************/

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.junit.JUnitCorePlugin;
import org.eclipse.jdt.internal.junit.JUnitMessages;
import org.eclipse.jdt.internal.junit.Messages;
import org.eclipse.jdt.internal.junit.launcher.ITestKind;
import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants;
import org.eclipse.jdt.internal.junit.launcher.JUnitRuntimeClasspathEntry;
import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;
import org.eclipse.jdt.internal.junit.util.CoreTestSearchEngine;
import org.eclipse.jdt.internal.junit.util.IJUnitStatusConstants;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.ExecutionArguments;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.osgi.framework.Bundle;

import br.usp.each.saeg.jaguar.plugin.JacocoAgentJar;
import br.usp.each.saeg.jaguar.plugin.JaguarConstants;
import br.usp.each.saeg.jaguar.plugin.ProjectUtils;
/**
 * Launch configuration delegate for a JUnit test as a Java application.
 *
 * <p>
 * Clients can instantiate and extend this class.
 * </p>
 * @since 3.3
 */
@SuppressWarnings("restriction")
public class JUnitLaunchConfigurationDelegate extends AbstractJavaLaunchConfigurationDelegate {

	private IMember[] fTestElements;

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.ILaunchConfigurationDelegate#launch(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String, org.eclipse.debug.core.ILaunch, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public synchronized void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		

		monitor.beginTask(new String[]{configuration.getName()} + "...", 5); //$NON-NLS-1$
		// check for cancellation
		if (monitor.isCanceled()) {
			return;
		}

		try {
			monitor.subTask(JUnitMessages.JUnitLaunchConfigurationDelegate_verifying_attriburtes_description);

			try {
				preLaunchCheck(configuration, launch, new SubProgressMonitor(monitor, 2));
			} catch (CoreException e) {
				if (e.getStatus().getSeverity() == IStatus.CANCEL) {
					monitor.setCanceled(true);
					return;
				}
				throw e;
			}
			// check for cancellation
			if (monitor.isCanceled()) {
				return;
			}

			fTestElements= evaluateTests(configuration, new SubProgressMonitor(monitor, 1));

			String mainTypeName= verifyMainTypeName(configuration);
			IVMRunner runner= getVMRunner(configuration, mode);

			File workingDir = verifyWorkingDirectory(configuration);
			String workingDirName = null;
			if (workingDir != null) {
				workingDirName= workingDir.getAbsolutePath();
			}

			// Environment variables
			String[] envp= getEnvironment(configuration);

			ArrayList<String> vmArguments= new ArrayList<String>();
			ArrayList<String> programArguments= new ArrayList<String>();
			collectExecutionArguments(configuration, vmArguments, programArguments);

			// Add jacoco agent to vm argument
			JacocoAgentJar jacocoAgent = new JacocoAgentJar();
			vmArguments.add(jacocoAgent.getVmArguments(
					configuration.getAttribute(JaguarConstants.ATTR_COVERAGE_TYPE, true),
					configuration.getAttribute(JaguarConstants.ATTR_INCLUDES, JaguarConstants.ATTR_INCLUDES_DEFAULT_VALUE)));
			
			// Extend VM Heap Space Max
			vmArguments.add("-Xmx1024m");
			
			// VM-specific attributes
			Map<String, Object> vmAttributesMap= getVMSpecificAttributesMap(configuration);
			
			// Classpath
			String[] classpath= getClasspath(configuration);

			// Create VM config
			VMRunnerConfiguration runConfig= new VMRunnerConfiguration(mainTypeName, classpath);
			runConfig.setVMArguments((String[]) vmArguments.toArray(new String[vmArguments.size()]));
			runConfig.setProgramArguments((String[]) programArguments.toArray(new String[programArguments.size()]));
			runConfig.setEnvironment(envp);
			runConfig.setWorkingDirectory(workingDirName);
			runConfig.setVMSpecificAttributesMap(vmAttributesMap);

			// Bootpath
			runConfig.setBootClassPath(getBootpath(configuration));

			// check for cancellation
			if (monitor.isCanceled()) {
				return;
			}

			// done the verification phase
			monitor.worked(1);

			monitor.subTask(JUnitMessages.JUnitLaunchConfigurationDelegate_create_source_locator_description);
			// set the default source locator if required
			setDefaultSourceLocator(launch, configuration);
			monitor.worked(1);

			// Launch the configuration - 1 unit of work
			runner.run(runConfig, launch, monitor);

			// check for cancellation
			if (monitor.isCanceled()) {
				return;
			}
		} finally {
			fTestElements= null;
			monitor.done();
		}
	}

	/**
	 * Performs a check on the launch configuration's attributes. If an attribute contains an invalid value, a {@link CoreException}
	 * with the error is thrown.
	 *
	 * @param configuration the launch configuration to verify
	 * @param launch the launch to verify
	 * @param monitor the progress monitor to use
	 * @throws CoreException an exception is thrown when the verification fails
	 */
	protected void preLaunchCheck(ILaunchConfiguration configuration, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		try {
			IJavaProject javaProject= getJavaProject(configuration);
			if ((javaProject == null) || !javaProject.exists()) {
				abort(JUnitMessages.JUnitLaunchConfigurationDelegate_error_invalidproject, null, IJavaLaunchConfigurationConstants.ERR_NOT_A_JAVA_PROJECT);
			}
			if (!CoreTestSearchEngine.hasTestCaseType(javaProject)) {
				abort(JUnitMessages.JUnitLaunchConfigurationDelegate_error_junitnotonpath, null, IJUnitStatusConstants.ERR_JUNIT_NOT_ON_PATH);
			}

			ITestKind testKind= getTestRunnerKind(configuration);
			boolean isJUnit4Configuration= TestKindRegistry.JUNIT4_TEST_KIND_ID.equals(testKind.getId());
			if (isJUnit4Configuration && ! CoreTestSearchEngine.hasJUnit4TestAnnotation(javaProject)) {
				abort(JUnitMessages.JUnitLaunchConfigurationDelegate_error_junit4notonpath, null, IJUnitStatusConstants.ERR_JUNIT_NOT_ON_PATH);
			}
		} finally {
			monitor.done();
		}
	}

	private ITestKind getTestRunnerKind(ILaunchConfiguration configuration) {
		ITestKind testKind= JUnitLaunchConfigurationConstants.getTestRunnerKind(configuration);
		if (testKind.isNull()) {
			testKind= TestKindRegistry.getDefault().getKind(TestKindRegistry.JUNIT3_TEST_KIND_ID); // backward compatible for launch configurations with no runner
		}
		return testKind;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate#verifyMainTypeName(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public String verifyMainTypeName(ILaunchConfiguration configuration) throws CoreException {
		return "br.usp.each.saeg.jaguar.core.cli.JaguarRunner4Eclipse"; //$NON-NLS-1$
	}

	/**
	 * Evaluates all test elements selected by the given launch configuration. The elements are of type
	 * {@link IType} or {@link IMethod}. At the moment it is only possible to run a single method or a set of types, but not
	 * mixed or more than one method at a time.
	 *
	 * @param configuration the launch configuration to inspect
	 * @param monitor the progress monitor
	 * @return returns all types or methods that should be ran
	 * @throws CoreException an exception is thrown when the search for tests failed
	 */
	protected IMember[] evaluateTests(ILaunchConfiguration configuration, IProgressMonitor monitor) throws CoreException {
		IJavaProject javaProject= getJavaProject(configuration);

		IJavaElement testTarget= getTestTarget(configuration, javaProject);
		String testMethodName= configuration.getAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_METHOD_NAME, ""); //$NON-NLS-1$
		if (testMethodName.length() > 0) {
			if (testTarget instanceof IType) {
				return new IMember[] { ((IType) testTarget).getMethod(testMethodName, new String[0]) };
			}
		}
		HashSet<IType> result= new HashSet<IType>();
		ITestKind testKind= getTestRunnerKind(configuration);
		testKind.getFinder().findTestsInContainer(testTarget, result, monitor);
		if (result.isEmpty()) {
			String msg= Messages.format(JUnitMessages.JUnitLaunchConfigurationDelegate_error_notests_kind, testKind.getDisplayName());
			abort(msg, null, IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_MAIN_TYPE);
		}
		return (IMember[]) result.toArray(new IMember[result.size()]);
	}

	/**
	 * Collects all VM and program arguments. Implementors can modify and add arguments.
	 *
	 * @param configuration the configuration to collect the arguments for
	 * @param vmArguments a {@link List} of {@link String} representing the resulting VM arguments
	 * @param programArguments a {@link List} of {@link String} representing the resulting program arguments
	 * @exception CoreException if unable to collect the execution arguments
	 */
	protected void collectExecutionArguments(ILaunchConfiguration configuration, List<String> vmArguments, List<String> programArguments) throws CoreException {

		// add program & VM arguments provided by getProgramArguments and getVMArguments
		String pgmArgs= getProgramArguments(configuration);
		String vmArgs= getVMArguments(configuration);
		ExecutionArguments execArgs= new ExecutionArguments(vmArgs, pgmArgs);
		vmArguments.addAll(Arrays.asList(execArgs.getVMArgumentsArray()));

		if (!configuration.getAttribute(JaguarConstants.ATTR_COVERAGE_TYPE, true)){
			programArguments.add("--dataflow"); //$NON-NLS-1$
		}
		
		String outputType = configuration.getAttribute(JaguarConstants.ATTR_OUTPUT_TYPE, JaguarConstants.ATTR_OUTPUT_TYPE_DEFAULT_VALUE);
		programArguments.add("--outputType"); //$NON-NLS-1$
		programArguments.add(outputType);
		
		programArguments.add("--logLevel"); //$NON-NLS-1$
		programArguments.add(configuration.getAttribute(JaguarConstants.ATTR_LOG_LEVEL, JaguarConstants.ATTR_LOG_LEVEL_DEFAULT_VALUE));
		
		programArguments.add("--projectDir"); //$NON-NLS-1$
		programArguments.add(getWorkingDirectory(configuration).getAbsolutePath());

		programArguments.add("--classesDir"); //$NON-NLS-1$
		
		// get default from java project
		String classesDir = configuration.getAttribute(JaguarConstants.ATTR_COMPILED_CLASSES_PATH, StringUtils.EMPTY);
		
		if (StringUtils.EMPTY.equals(classesDir)){
			IJavaProject javaProject = getJavaProject(configuration);
			IPath outputLocation = javaProject.getOutputLocation();
			classesDir = javaProject.getResource().getWorkspace().getRoot().findMember(outputLocation).getLocation().toOSString();
		}

		programArguments.add(classesDir);

		IMember[] testElements = fTestElements;
		String fileName= createTestNamesFile(testElements);
		programArguments.add("--testsListFile"); //$NON-NLS-1$
		programArguments.add(fileName);
		
	}

	private String createTestNamesFile(IMember[] testElements) throws CoreException {
		try {
			File file= File.createTempFile("testNames", ".txt"); //$NON-NLS-1$ //$NON-NLS-2$
			file.deleteOnExit();
			BufferedWriter bw= null;
			try {
				bw= new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8")); //$NON-NLS-1$
				for (int i= 0; i < testElements.length; i++) {
					if (testElements[i] instanceof IType) {
						IType type= (IType) testElements[i];
						String testName= type.getFullyQualifiedName();
						bw.write(testName);
						bw.newLine();
					} else {
						abort(JUnitMessages.JUnitLaunchConfigurationDelegate_error_wrong_input, null, IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_MAIN_TYPE);
					}
				}
			} finally {
				if (bw != null) {
					bw.close();
				}
			}
			return file.getAbsolutePath();
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, JUnitCorePlugin.CORE_PLUGIN_ID, IStatus.ERROR, "", e)); //$NON-NLS-1$
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate#getClasspath(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public String[] getClasspath(ILaunchConfiguration configuration) throws CoreException {
		String[] cp= super.getClasspath(configuration);

		ITestKind kind= getTestRunnerKind(configuration);
		List<String> junitEntries = new ClasspathLocalizer(Platform.inDevelopmentMode()).localizeClasspath(kind);
		
		String[] classPath= new String[cp.length + junitEntries.size() + 1];
		Object[] jea= junitEntries.toArray();
		System.arraycopy(cp, 0, classPath, 0, cp.length);
		System.arraycopy(jea, 0, classPath, cp.length, jea.length);
		
		classPath[classPath.length - 1] = ProjectUtils.getLibJarLocation("br.usp.each.saeg.jaguar.core.jar"); //$NON-NLS-1$
		return classPath;
	}

	private static class ClasspathLocalizer {

		private boolean fInDevelopmentMode;

		public ClasspathLocalizer(boolean inDevelopmentMode) {
			fInDevelopmentMode = inDevelopmentMode;
		}

		public List<String> localizeClasspath(ITestKind kind) {
			JUnitRuntimeClasspathEntry[] entries= kind.getClasspathEntries();
			List<String> junitEntries= new ArrayList<String>();

			for (int i= 0; i < entries.length; i++) {
				try {
					addEntry(junitEntries, entries[i]);
				} catch (IOException e) {
					Assert.isTrue(false, entries[i].getPluginId() + " is available (required JAR)"); //$NON-NLS-1$
				}
			}
			return junitEntries;
		}

		private void addEntry(List<String> junitEntries, final JUnitRuntimeClasspathEntry entry) throws IOException, MalformedURLException {
			String entryString= entryString(entry);
			if (entryString != null)
				junitEntries.add(entryString);
		}

		private String entryString(final JUnitRuntimeClasspathEntry entry) throws IOException, MalformedURLException {
			if (inDevelopmentMode()) {
				try {
					return localURL(entry.developmentModeEntry());
				} catch (IOException e3) {
					// fall through and try default
				}
			}
			return localURL(entry);
		}

		private boolean inDevelopmentMode() {
			return fInDevelopmentMode;
		}

		private String localURL(JUnitRuntimeClasspathEntry jar) throws IOException, MalformedURLException {
			Bundle bundle= JUnitCorePlugin.getDefault().getBundle(jar.getPluginId());
			URL url;
			if (jar.getPluginRelativePath() == null)
				url= bundle.getEntry("/"); //$NON-NLS-1$
			else
				url= bundle.getEntry(jar.getPluginRelativePath());
			if (url == null)
				throw new IOException();
			return FileLocator.toFileURL(url).getFile();
		}
	}

	private final IJavaElement getTestTarget(ILaunchConfiguration configuration, IJavaProject javaProject) throws CoreException {
		String containerHandle = configuration.getAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_CONTAINER, ""); //$NON-NLS-1$
		if (containerHandle.length() != 0) {
			 IJavaElement element= JavaCore.create(containerHandle);
			 if (element == null || !element.exists()) {
				 abort(JUnitMessages.JUnitLaunchConfigurationDelegate_error_input_element_deosn_not_exist, null, IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_MAIN_TYPE);
			 }
			 return element;
		}
		String testTypeName= getMainTypeName(configuration);
		if (testTypeName != null && testTypeName.length() != 0) {
			IType type= javaProject.findType(testTypeName);
			if (type != null && type.exists()) {
				return type;
			}
		}
		abort(JUnitMessages.JUnitLaunchConfigurationDelegate_input_type_does_not_exist, null, IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_MAIN_TYPE);
		return null; // not reachable
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.junit.launcher.ITestFindingAbortHandler#abort(java.lang.String, java.lang.Throwable, int)
	 */
	protected void abort(String message, Throwable exception, int code) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, JUnitCorePlugin.CORE_PLUGIN_ID, code, message, exception));
	}

}
