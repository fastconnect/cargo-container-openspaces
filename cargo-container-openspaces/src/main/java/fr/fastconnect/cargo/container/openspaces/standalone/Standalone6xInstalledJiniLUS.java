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

import net.jini.core.discovery.LookupLocator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Path;
import org.codehaus.cargo.container.ContainerCapability;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.deployable.DeployableType;
import org.codehaus.cargo.container.internal.AntContainerExecutorThread;
import org.codehaus.cargo.container.spi.AbstractInstalledLocalContainer;

/**
 * Implementation of {@link org.codehaus.cargo.container.InstalledLocalContainer} that starts a JINI LUS
 * This is using GigaSpaces service-grid starter classes.
 */
public class Standalone6xInstalledJiniLUS extends AbstractInstalledLocalContainer {
	private final static Log logger = LogFactory.getLog(Standalone6xInstalledJiniLUS.class);

	private AntContainerExecutorThread puInstanceRunner;

	public Standalone6xInstalledJiniLUS(final LocalConfiguration configuration) {
		super(configuration);
	}

	public String getId() {
		return "standalone-installed-lus-6x";
	}

	public String getName() {
		return "Standalone Installed Jini Lookup Service 6.x";
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
		logger.debug("prepare LUS environment...");

		Path classpath = java.createClasspath();

		classpath.createPathElement().setPath(getHome());
		classpath.createPathElement().setLocation(new File(getHome(), "/lib/ServiceGrid/gs-boot.jar"));
		classpath.createPathElement().setLocation(new File(getHome(), "/lib/jini/start.jar"));
		classpath.createPathElement().setLocation(new File(getHome(), "/lib/jini/jsk-lib.jar"));
		classpath.createPathElement().setLocation(new File(getHome(), "/lib/jini/jsk-platform.jar"));
		classpath.createPathElement().setLocation(new File(getHome(), "/lib/jini/tools.jar"));
		classpath.createPathElement().setLocation(new File(getHome(), "/lib/jini/reggie.jar"));
		classpath.createPathElement().setLocation(new File(getHome(), "/lib/jini/jsk-resources.jar"));
		classpath.createPathElement().setLocation(new File(getHome(), "/lib/jini/jsk-policy.jar"));
		classpath.createPathElement().setLocation(new File(getHome(), "/lib/jini/mahalo.jar"));

		addToolsJarToClasspath(classpath);
		java.setClassname("com.gigaspaces.start.SystemBoot");

		java.createArg().setValue("com.gigaspaces.start.services=\"LH\"");
		puInstanceRunner = new AntContainerExecutorThread(java);
		
		logger.info("Starting ant container executor thread");
		
		puInstanceRunner.start();
	}

	@Override
	protected void waitForCompletion(final boolean waitForStarting) {
		logger.debug("Checking LUS");
		boolean wait = true;
		while(wait) {
			LookupLocator lookup;
			try {
				lookup = new LookupLocator("jini://localhost");
				if(waitForStarting) { // wait for the lookup to start
					if(lookup != null) {
						logger.info("Lookup locator found at <" + lookup.getHost() + ":" + lookup.getPort() + ">");
						wait = false;
					}
				} else { // wait for the lookup to stop
					if(lookup == null) {
						logger.info("Lookup locator is null");
						wait = false;
					}
				}
			} catch(java.net.MalformedURLException e) {
				logger.error("Unexpected Exception caught.", e);
				wait = false;
			}
		}
	}

	@Override
	protected void doStop(final Java java) throws Exception {
		logger.info("Stopping LUS");
		if(puInstanceRunner!=null) {
			logger.info("using thread.interrupt");
			this.puInstanceRunner.interrupt();
			synchronized (this) {
				notifyAll();
			}
		}
	}
}