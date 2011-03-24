/**
 * Copyright (C) 2009 FastConnect
 *
 * Cargo OpenSpaces Container is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Cargo OpenSpaces Container is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/lgpl.txt>.
 */

package fr.fastconnect.cargo.container.openspaces.installed;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Environment.Variable;
import org.apache.tools.ant.types.Path;
import org.codehaus.cargo.container.ContainerCapability;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.deployable.DeployableType;
import org.codehaus.cargo.container.internal.AntContainerExecutorThread;
import org.codehaus.cargo.container.spi.AbstractInstalledLocalContainer;

import fr.fastconnect.cargo.container.openspaces.OpenSpacesContainerHelper;
import fr.fastconnect.cargo.container.openspaces.deployable.ProcessingUnitDeployable;

/**
 * Start a GigaSpaces grid (GSC/GSM) according to the given configuration. This is using the Grid Service Agent
 * capabilities
 * <p/>
 * Starting 2 containers with different configuration is apparently not supported.
 * http://article.gmane.org/gmane.comp.java.cargo.user/2001
 * <p/>
 * http://cargo.codehaus.org/Features
 * 
 * @author luc boutier
 */
public class OpenSpaces7xInstalledLocalContainer extends AbstractInstalledLocalContainer {
	private final static Log logger = LogFactory.getLog(OpenSpaces7xInstalledLocalContainer.class);

	/**
	 * Unique container id.
	 */
	public static final String ID = "openspaces7x";
	private static final String NAME = "OpenSpaces 7.x Installed Local Container";

	private static final String JSHOMEDIR_ENV_PROP = "JSHOMEDIR";

	private AntContainerExecutorThread gsaContainerExecutorThread;
	private OpenSpacesContainerHelper openSpacesContainerHelper;

	@SuppressWarnings("unchecked")
	public OpenSpaces7xInstalledLocalContainer(final LocalConfiguration configuration) {
		super(configuration);
		openSpacesContainerHelper = new OpenSpacesContainerHelper(getConfiguration().getPropertyValue(
				OpensSpaces7xInstalledLocalConfiguration.CARGO_LOOKUP_LOCATORS), getConfiguration().getPropertyValue(
				OpensSpaces7xInstalledLocalConfiguration.CARGO_LOOKUP_GROUPS), null, null, getConfiguration()
				.getDeployables());
	}

	private void prepareJava(Java java) {
		// set GigaSpaces ClassPath
		Path classpath = java.createClasspath();

		classpath.createPathElement().setPath(getHome());
		classpath.createPathElement().setLocation(new File(getHome(), "lib/platform/jdbc/h2.jar"));
		classpath.createPathElement().setLocation(new File(getHome(), "platform/jdbc/hsqldb.jar"));
		classpath.createPathElement().setLocation(new File(getHome(), "lib/platform/sigar/*.jar"));
		classpath.createPathElement().setLocation(new File(getHome(), "lib/platform/jmx/jmxremote.jar"));
		classpath.createPathElement().setLocation(new File(getHome(), "lib/platform/jmx/jmxri.jar"));
		classpath.createPathElement().setLocation(new File(getHome(), "lib/platform/jmx/jmxtools.jar"));
		classpath.createPathElement().setLocation(new File(getHome(), "lib/platform/boot/gs-boot.jar"));

		// set the required java properties
		Variable variable = new Variable();
		final String licenseKey = getConfiguration().getPropertyValue(
				OpensSpaces7xInstalledLocalConfiguration.CARGO_LICENSEKEY);
		if (licenseKey != null) {
			variable.setKey(OpensSpaces7xInstalledLocalConfiguration.CARGO_LICENSEKEY);
			variable.setValue(licenseKey);
			java.addSysproperty(variable);
		}

		final String lookupGroups = getConfiguration().getPropertyValue(
				OpensSpaces7xInstalledLocalConfiguration.CARGO_LOOKUP_GROUPS);
		if (lookupGroups != null) {
			variable = new Variable();
			variable.setKey(OpensSpaces7xInstalledLocalConfiguration.CARGO_LOOKUP_GROUPS);
			variable.setValue(lookupGroups);
			java.addSysproperty(variable);

			variable = new Variable();
			variable.setKey("LOOKUPGROUPS");
			variable.setValue(lookupGroups);
			java.addEnv(variable);
		}

		final String lookupLocators = getConfiguration().getPropertyValue(
				OpensSpaces7xInstalledLocalConfiguration.CARGO_LOOKUP_LOCATORS);
		if (lookupLocators != null) {
			variable = new Variable();
			variable.setKey(OpensSpaces7xInstalledLocalConfiguration.CARGO_LOOKUP_LOCATORS);
			variable.setValue(lookupLocators);
			java.addSysproperty(variable);

			variable = new Variable();
			variable.setKey("LOOKUPLOCATORS");
			variable.setValue(lookupLocators);
			java.addEnv(variable);
		}

		logger.info("Starting GSA using locators <" + lookupLocators + "> and groups <" + lookupGroups + ">");
		System.out.println("and sys prop test: " + getSystemProperties().get("test"));

		variable = new Variable();
		variable.setKey(JSHOMEDIR_ENV_PROP);
		variable.setValue(getHome());
		java.addEnv(variable);

		// prepare agent startup class
		java.setClassname("com.gigaspaces.start.SystemBoot");
		java.createArg().setValue("com.gigaspaces.start.services=\"GSA\"");

		// set agent grid parameter
		final String agentConfiguration = getConfiguration().getPropertyValue(
				OpensSpaces7xInstalledLocalConfiguration.CARGO_AGENTCONFIGURATION);
		if (agentConfiguration != null) {
			java.setArgs(agentConfiguration);
		}
	}

	@Override
	protected void doStart(Java java) throws Exception {
		logger.info("Starting Grid Service Agent");

		// set the classPath and system properties
		prepareJava(java);

		// start the java in a task-thread
		gsaContainerExecutorThread = new AntContainerExecutorThread(java);
		gsaContainerExecutorThread.setName("GSA Container Executor Thread");

		logger.info("Starting ant container executor thread");
		gsaContainerExecutorThread.start();
	}

	@Override
	protected void waitForCompletion(boolean waitForStarting) throws InterruptedException {
		openSpacesContainerHelper.waitForCompletion(waitForStarting);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void doStop(Java java) throws Exception {
		logger.info("Stopping Grid Service Agent");
		gsaContainerExecutorThread.stop();
	}

	public ContainerCapability getCapability() {
		return new ContainerCapability() {
			public boolean supportsDeployableType(final DeployableType type) {
				return type.equals(ProcessingUnitDeployable.getDeployableType());
			}
		};
	}

	public String getId() {
		return ID;
	}

	public String getName() {
		return NAME;
	}
}
