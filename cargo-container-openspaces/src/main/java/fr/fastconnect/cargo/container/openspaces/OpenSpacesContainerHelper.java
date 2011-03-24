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
package fr.fastconnect.cargo.container.openspaces;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.cargo.container.deployable.Deployable;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.gsa.GridServiceAgent;

import com.gigaspaces.grid.gsa.AgentProcessDetails;

/**
 * This class is the common tool for both installed or remote OpenSpaces containers.
 */
public class OpenSpacesContainerHelper {
	/** logger. */
	private static final Log logger = LogFactory.getLog(OpenSpacesContainerHelper.class);
	/** Timeout to wait for grid to start and processing units to be deployed. */
	public static final int START_WAIT_TIMEOUT = 120000;
	/** Timeout to wait for the grid to stop. */
	public static final int STOP_WAIT_TIMEOUT = 60000;
	/** JINI locators. */
	private final String lookupLocators;
	/** JINI lookup groups. */
	private final String lookupGroups;
	/** User to manage the grid. */
	private final String gridUser;
	/** Password to manage the grid. */
	private final String gridPassword;
	/** List of processing unit to deploy. */
	private final List<Deployable> deployables;
	/** Utility class that is responsible for processing unit deployment. */
//	private final ProcessingUnitDeployer deployer;

	public OpenSpacesContainerHelper(final String lookupLocators, final String lookupGroups, final String gridUser,
			final String gridPassword, final List<Deployable> deployables) {
		this.lookupLocators = lookupLocators;
		this.lookupGroups = lookupGroups;
		this.deployables = deployables;
		this.gridUser = gridUser;
		this.gridPassword = gridPassword;
//		this.deployer = new ProcessingUnitDeployer();
	}

	/**
	 * Wait for grid to start or stop. This includes both the grid startup as well as processing unit deployment.
	 * 
	 * @param waitForStarting
	 *            true if we wait for starting, false if we wait for stopping.
	 * @throws InterruptedException
	 *             in case the thread is interrupted.
	 */
	public void waitForCompletion(boolean waitForStarting) throws InterruptedException {
		final Admin admin = getAdmin();
		try {
			if (waitForStarting) {
				waitGridStart(admin);
			} else {
				waitGridStop(admin);
			}
		} finally {
			admin.close();
		}
	}

	public void waitGridStop(Admin admin) throws InterruptedException {
		logger.info("Waiting for grid to stop");
		// while (admin.getGridServiceAgents().waitFor(1, STOP_WAIT_TIMEOUT, TimeUnit.MILLISECONDS)) {
		// Thread.sleep(STOP_WAIT_TIMEOUT);
		// }
		logger.info("Grid stopped");
	}

	public void waitGridStart(Admin admin) throws InterruptedException {
		logger.info("Waiting for grid to start");
		logger.info("Waiting for GSA");
		GridServiceAgent gsa = admin.getGridServiceAgents()
				.waitForAtLeastOne(START_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
		if (gsa == null) {
			throw new InterruptedException("timeout occured before GSA is started");
		}
		logger.info("GSA found");

		int gscCount = 0;
		int gsmCount = 0;
		int lusCount = 0;
		Iterator<AgentProcessDetails> processIterator = gsa.getProcessesDetails().iterator();
		while (processIterator.hasNext()) {
			AgentProcessDetails apd = processIterator.next();
			if (apd.getServiceType().equals("gsc")) {
				gscCount++;
			}
			if (apd.getServiceType().equals("gsm")) {
				gsmCount++;
			}
			if (apd.getServiceType().equals("lus")) {
				lusCount++;
			}
		}
		// wait for grid to be started
		if (!(admin.getGridServiceContainers().waitFor(gscCount, START_WAIT_TIMEOUT, TimeUnit.MILLISECONDS)
				&& admin.getGridServiceManagers().waitFor(gsmCount, START_WAIT_TIMEOUT, TimeUnit.MILLISECONDS) && admin
				.getLookupServices().waitFor(lusCount, START_WAIT_TIMEOUT, TimeUnit.MILLISECONDS))) {
			final int discoveredGSC = admin.getGridServiceContainers().getSize();
			final int discoveredGSM = admin.getGridServiceManagers().getSize();
			final int discoveredLUS = admin.getLookupServices().getSize();
			throw new InterruptedException("Timeout occured before Grid components are started; " + "discovered <"
					+ discoveredGSC + "> GSC, <" + discoveredGSM + "> GSM and <" + discoveredLUS
					+ "> LUS using locators <" + Arrays.toString(admin.getLocators()) + "> and groups <"
					+ Arrays.toString(admin.getGroups()) + ">" + "but was expecting <" + gscCount + "> GSC, <"
					+ gsmCount + "> GSM and <" + lusCount + "> LUS.");
		}
		logger.info("Grid started.");
		// if there is some deployables to deploy deploy them here
		deployProcessingUnits(admin);
		logger.info("Processing units deployed.");
	}

	/**
	 * Deploy all processing units configured for Cargo deployment.
	 * 
	 * @param admin
	 *            The admin API to use to deploy pu.
	 */
	private void deployProcessingUnits(Admin admin) {
		if (deployables == null) {
			logger.warn("No deployable found!");
			return;
		}
		// deploy all processing units on the grid
		for (Deployable deployable : deployables) {
//			deployer.deployProcessingUnit(admin, deployable);
		}
	}

	// TODO undeploy processing unit
	// private undeployProcessingUnits(Admin admin) {
	// if (deployables == null) {
	// logger.warn("No deployable found!");
	// return;
	// }
	// // deploy all processing units on the grid
	// for (Deployable deployable : deployables) {
	// deployer.deployProcessingUnit(admin, deployable.get);
	// }
	// }

	public Admin getAdmin() {
		return getAdmin(this.lookupLocators, this.lookupGroups, this.gridUser, this.gridPassword);
	}

	public static Admin getAdmin(String locators, String groups) {
		return getAdmin(locators, groups, null, null);
	}

	public static Admin getAdmin(final String locators, final String groups, final String gridUser,
			final String gridPassword) {
		final AdminFactory adminFactory = new AdminFactory();
		if (groups != null) {
			adminFactory.addGroups(groups);
		}
		if (locators != null) {
			adminFactory.addLocators(locators);
		}
		if (gridUser != null) {
			adminFactory.userDetails(gridUser, gridPassword);
		}
		return adminFactory.createAdmin();
	}
}
