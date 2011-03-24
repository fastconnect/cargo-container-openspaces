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

package fr.fastconnect.cargo.container.openspaces.standalone;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Path;
import org.codehaus.cargo.container.ContainerCapability;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.deployable.DeployableType;
import org.codehaus.cargo.container.internal.AntContainerExecutorThread;
import org.codehaus.cargo.container.spi.AbstractInstalledLocalContainer;

import fr.fastconnect.maven.cargo.container.openspaces.ProcessingUnitDeployable;

/**
 * Implementation of {@link org.codehaus.cargo.container.InstalledLocalContainer}
 * that starts {@link org.openspaces.pu.container.standalone.StandaloneProcessingUnitContainer} in a new {@link org.codehaus.cargo.container.internal.AntContainerExecutorThread}
 */
public class Standalone6xInstalledLocalContainer extends AbstractInstalledLocalContainer {
	private final static Log logger = LogFactory.getLog(Standalone6xInstalledLocalContainer.class);
	private final static long GS_START_WAIT_TIME=10000;
	
	private AntContainerExecutorThread puInstanceRunner;
	
	public Standalone6xInstalledLocalContainer(final LocalConfiguration configuration) {
		super(configuration);
	}
	
	public String getId() {
		return "standalone-installed-puc-6x";
	}
	
	public String getName() {
		return "Standalone Installed ProcessingUnitContainer 6.x";
	}

	public ContainerCapability getCapability() {
		return new ContainerCapability() {
			public boolean supportsDeployableType(final DeployableType type) {
				return false;
			}
		};
	}
	
	@Override
	protected void doStart(final Java java) throws Exception {
		logger.info("Prepare to start a StandaloneProcessingUnitContainer");
		
		Path classpath = java.createClasspath();
		classpath.createPathElement().setLocation(new File(getHome(), "/lib/spring/spring.jar"));
		classpath.createPathElement().setLocation(new File(getHome(), "/lib/JSpaces.jar"));
		classpath.createPathElement().setLocation(new File(getHome(), "/lib/openspaces/openspaces.jar"));
		classpath.createPathElement().setLocation(new File(getHome(), "/lib/common/commons-logging.jar"));

		addToolsJarToClasspath(classpath);
		java.setClassname("org.openspaces.pu.container.standalone.StandaloneProcessingUnitContainer");
		
		final ProcessingUnitDeployable deployable = (ProcessingUnitDeployable) getConfiguration().getDeployables().get(0);
		
		StringBuilder puInstanceArgs = new StringBuilder();
		if(deployable.getCluster()!=null && !deployable.getCluster().equals("")) {
			puInstanceArgs.append("-cluster ");
			puInstanceArgs.append(deployable.getCluster());
			puInstanceArgs.append(" ");
		}
		if(deployable.getProperties()!=null && !deployable.getProperties().equals("")) {
			puInstanceArgs.append("-properties ");
			puInstanceArgs.append(deployable.getProperties());
			puInstanceArgs.append(" ");
		}
		
		puInstanceArgs.append(deployable.getFile());
		String args = puInstanceArgs.toString();

		java.setArgs(args);
		puInstanceRunner = new AntContainerExecutorThread(java);
		
		if(logger.isInfoEnabled()){
			logger.info("Starting standalone processing unit container with arguments <" + args + ">");
		}
		
		puInstanceRunner.start();
	}

	@Override
	protected void waitForCompletion(final boolean waitForStarting) throws InterruptedException {
		// TODO test processing unit startup
		Thread.sleep(Standalone6xInstalledLocalContainer.GS_START_WAIT_TIME);
		logger.info("GigaSpaces container started");
	}

	@Override
	protected void doStop(final Java java) throws Exception {
		logger.info("Stopping GigaSpaces container");
		if(puInstanceRunner!=null) {
			this.puInstanceRunner.interrupt();
		}
	}
}