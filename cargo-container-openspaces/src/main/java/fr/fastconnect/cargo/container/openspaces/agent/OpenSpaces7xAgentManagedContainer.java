///**
// * Copyright (C) 2009 FastConnect
// *
// * Cargo OpenSpaces Container is free software: you can redistribute it and/or modify
// * it under the terms of the GNU Lesser General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * Cargo OpenSpaces Container is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU Lesser General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/lgpl.txt>.
// */
//
//package fr.fastconnect.cargo.container.openspaces.agent;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.concurrent.TimeUnit;
//
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
//import org.apache.tools.ant.taskdefs.Java;
//import org.codehaus.cargo.container.ContainerCapability;
//import org.codehaus.cargo.container.configuration.LocalConfiguration;
//import org.codehaus.cargo.container.deployable.Deployable;
//import org.codehaus.cargo.container.deployable.DeployableType;
//import org.codehaus.cargo.container.spi.AbstractInstalledLocalContainer;
//import org.openspaces.admin.Admin;
//import org.openspaces.admin.gsa.GridServiceAgent;
//import org.openspaces.admin.gsa.GridServiceAgents;
//import org.openspaces.admin.gsa.GridServiceContainerOptions;
//import org.openspaces.admin.gsa.GridServiceManagerOptions;
//import org.openspaces.admin.gsc.GridServiceContainer;
//import org.openspaces.admin.gsm.GridServiceManager;
//import org.openspaces.admin.pu.DeploymentStatus;
//import org.openspaces.admin.pu.ProcessingUnit;
//
//import fr.fastconnect.cargo.container.openspaces.OpenSpacesContainerHelper;
//import fr.fastconnect.cargo.container.openspaces.ProcessingUnitDeployer;
//import fr.fastconnect.cargo.container.openspaces.deployable.ProcessingUnitDeployable;
//
///**
// * Start a GigaSpaces grid (GSC/GSM) according to the given configuration. This is using the Grid Service Agent
// * capabilities. A running gsAgent should be available using the given jini locators/groups.
// * <p/>
// * Starting 2 containers with different configuration is apparently not supported.
// * http://article.gmane.org/gmane.comp.java.cargo.user/2001
// * <p/>
// * http://cargo.codehaus.org/Features
// * 
// * @author luc boutier
// */
//public class OpenSpaces7xAgentManagedContainer extends AbstractInstalledLocalContainer {
//
//	
//	/** provision user */
//	private String provisionUser;
//	/** provision password */
//	private String provisionPassword;
//	/** GigaSpaces admin API. */
//	private Admin admin;
//	/** list of deployed processing units. */
//	private List<ProcessingUnit> puList = new ArrayList<ProcessingUnit>();
//
//	public OpenSpaces7xAgentManagedContainer(final LocalConfiguration configuration) {
//		super(configuration);
//	}
//
//	@Override
//	protected void doStart(Java java) throws Exception {
//		LOGGER.info("Connecting to GigaSpaces Agent...");
//
//		if (this.admin == null) {
//			admin = OpenSpacesContainerHelper.getAdmin(this.lookupLocators, this.lookupGroups, this.gridUser,
//					this.gridPassword);
//		}
//
//		// get the different agents part of the grid
//		GridServiceAgents agents = admin.getGridServiceAgents();
//		agents.waitFor(this.expectedGSA, OpenSpacesContainerHelper.START_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
//
//		// get the available agents to deploy the grid on...
//		GridServiceAgent[] gsas = agents.getAgents();
//
//		LOGGER.info("Starting grid...");
//		startGrid(gsas);
//		// waiting for services to be indeed started
//		waitGridStart();
//		LOGGER.info("Grid started successfully");
//		LOGGER.info("Requesting pu deployments");
//		deployProcessingUnits();
//		LOGGER.info("pu deployment requested");
//	}
//
//	/**
//	 * Now we should deploy GSMs and GSCs on the grid. Current implementation just support random deployment but we
//	 * should make something smarter.
//	 * 
//	 * @param gsas
//	 *            The agents on which to start the GSMs and GSCs
//	 */
//	private void startGrid(GridServiceAgent[] gsas) {
//		GridServiceManagerOptions gsmopt = new GridServiceManagerOptions();
//		int agentIncrement = 0;
//		for (int i = 0; i < this.gsmCount; i++) {
//			gsas[agentIncrement].startGridService(gsmopt);
//			agentIncrement++;
//			if (agentIncrement <= gsas.length) {
//				agentIncrement = 0;
//			}
//		}
//
//		GridServiceContainerOptions gscContainerOptions = new GridServiceContainerOptions();
//		if (this.gscVmOptions != null) {
//			gscContainerOptions.vmInputArgument(this.gscVmOptions);
//		}
//		agentIncrement = 0;
//		for (int i = 0; i < this.gscCount; i++) {
//			gsas[agentIncrement].startGridService(gscContainerOptions);
//			agentIncrement++;
//			if (agentIncrement <= gsas.length) {
//				agentIncrement = 0;
//			}
//		}
//	}
//
//	/**
//	 * Wait for the processes to be indeed started and discovered.
//	 * 
//	 * @throws InterruptedException
//	 *             In case the grid fail to start within the defined timeout.
//	 */
//	private void waitGridStart() throws InterruptedException {
//		if (this.admin == null) {
//			admin = OpenSpacesContainerHelper.getAdmin(this.lookupLocators, this.lookupGroups, this.gridUser,
//					this.gridPassword);
//		}
//
//		// wait for grid to be started
//		if (!(this.admin.getGridServiceContainers().waitFor(gscCount, OpenSpacesContainerHelper.START_WAIT_TIMEOUT,
//				TimeUnit.MILLISECONDS) && this.admin.getGridServiceManagers().waitFor(gsmCount,
//				OpenSpacesContainerHelper.START_WAIT_TIMEOUT, TimeUnit.MILLISECONDS))) {
//			final int discoveredGSC = this.admin.getGridServiceContainers().getSize();
//			final int discoveredGSM = this.admin.getGridServiceManagers().getSize();
//			throw new InterruptedException("Timeout occured before Grid components are started; " + "discovered <"
//					+ discoveredGSC + "> GSC, <" + discoveredGSM + "> GSM using locators <"
//					+ Arrays.toString(this.admin.getLocators()) + "> and groups <"
//					+ Arrays.toString(this.admin.getGroups()) + ">" + "but was expecting <" + this.gscCount
//					+ "> GSC, <" + this.gsmCount + "> GSM");
//		}
//	}
//
//	private void deployProcessingUnits() {
//		if (this.deployables == null) {
//			LOGGER.warn("No deployable found!");
//			return;
//		}
//		if (provisionUser != null) {
//			admin.close();
//			admin = OpenSpacesContainerHelper.getAdmin(this.lookupLocators, this.lookupGroups, this.provisionUser,
//					this.provisionPassword);
//		}
//		ProcessingUnitDeployer deployer = new ProcessingUnitDeployer();
//		// deploy all processing units on the grid
//		for (Deployable deployable : deployables) {
//			puList.add(deployer.deployProcessingUnit(admin, deployable));
//		}
//	}
//
//	@Override
//	protected void waitForCompletion(boolean waitForStarting) throws InterruptedException {
//		LOGGER.info("waiting for pu to be deploted...");
//		// wait for processing units to be deployed
//		for (ProcessingUnit pu : puList) {
//			ProcessingUnitDeployer.waitForStatus(pu, OpenSpacesContainerHelper.START_WAIT_TIMEOUT,
//					DeploymentStatus.INTACT);
//		}
//		LOGGER.info("Processing units are in status intact!");
//	}
//
//	@Override
//	protected void doStop(Java java) throws Exception {
//		if (LOGGER.isInfoEnabled()) {
//			LOGGER.info("Connecting to gsAgent...");
//		}
//		// stop the GSM/ GSC etc.. on this agent.
//		if (this.admin != null) {
//			// undeploy the processing units if required.
//			if (gridUser != null && provisionUser != null && !gridUser.equals(provisionUser)) {
//				// stop the admin that has deploy rights
//				admin.close();
//				// we need a fresh admin with correct users privileges.
//				this.admin = OpenSpacesContainerHelper.getAdmin(this.lookupLocators, this.lookupGroups, this.gridUser,
//						this.gridPassword);
//			}
//		} else {
//			this.admin = OpenSpacesContainerHelper.getAdmin(this.lookupLocators, this.lookupGroups, this.gridUser,
//					this.gridPassword);
//		}
//
//		// get the different agents part of the grid
//		GridServiceAgents agents = this.admin.getGridServiceAgents();
//		agents.waitFor(this.expectedGSA, OpenSpacesContainerHelper.START_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
//
//		LOGGER.info("agent found - stopping grid");
//		// wait for GSCs to be discovered
//		this.admin.getGridServiceContainers().waitFor(this.gscCount, OpenSpacesContainerHelper.START_WAIT_TIMEOUT,
//				TimeUnit.MILLISECONDS);
//		// wait for GSMs to be discovered
//		this.admin.getGridServiceManagers().waitFor(this.gsmCount, OpenSpacesContainerHelper.START_WAIT_TIMEOUT,
//				TimeUnit.MILLISECONDS);
//
//		// kill gscs
//		LOGGER.info("agent found - stopping gscs");
//		GridServiceContainer[] containers = this.admin.getGridServiceContainers().getContainers();
//		for (GridServiceContainer container : containers) {
//			container.getGridServiceAgent().killByAgentId(container.getAgentId());
//		}
//		// kill gsms
//		LOGGER.info("agent found - stopping gsms");
//		GridServiceManager[] managers = this.admin.getGridServiceManagers().getManagers();
//		for (GridServiceManager manager : managers) {
//			manager.getGridServiceAgent().killByAgentId(manager.getAgentId());
//		}
//
//		admin.close();
//
//		LOGGER.info("grid stopped successfully");
//	}
//
//	public ContainerCapability getCapability() {
//		return new ContainerCapability() {
//			public boolean supportsDeployableType(final DeployableType type) {
//				return type.equals(ProcessingUnitDeployable.getDeployableType());
//			}
//		};
//	}
//
//	public String getId() {
//		return ID;
//	}
//
//	public String getName() {
//		return NAME;
//	}
//
//	public String getLookupGroups() {
//		return lookupGroups;
//	}
//
//	public void setLookupGroups(String lookupGroups) {
//		this.lookupGroups = lookupGroups;
//	}
//
//	public String getLookupLocators() {
//		return lookupLocators;
//	}
//
//	public void setLookupLocators(String lookupLocators) {
//		this.lookupLocators = lookupLocators;
//	}
//
//	public int getExpectedGSA() {
//		return expectedGSA;
//	}
//
//	public void setExpectedGSA(int expectedGSA) {
//		this.expectedGSA = expectedGSA;
//	}
//
//	public int getGsmCount() {
//		return gsmCount;
//	}
//
//	public void setGsmCount(int gsmCount) {
//		this.gsmCount = gsmCount;
//	}
//
//	public int getGscCount() {
//		return gscCount;
//	}
//
//	public void setGscCount(int gscCount) {
//		this.gscCount = gscCount;
//	}
//
//	public String getGscVmOptions() {
//		return gscVmOptions;
//	}
//
//	public void setGscVmOptions(String gscVmOptions) {
//		this.gscVmOptions = gscVmOptions;
//	}
//
//	public String getGridUser() {
//		return gridUser;
//	}
//
//	public void setGridUser(String gridUser) {
//		this.gridUser = gridUser;
//	}
//
//	public String getGridPassword() {
//		return gridPassword;
//	}
//
//	public void setGridPassword(String gridPassword) {
//		this.gridPassword = gridPassword;
//	}
//
//	public String getProvisionUser() {
//		return provisionUser;
//	}
//
//	public void setProvisionUser(String provisionUser) {
//		this.provisionUser = provisionUser;
//	}
//
//	public String getProvisionPassword() {
//		return provisionPassword;
//	}
//
//	public void setProvisionPassword(String provisionPassword) {
//		this.provisionPassword = provisionPassword;
//	}
//
//	public List<Deployable> getDeployables() {
//		return deployables;
//	}
//
//	public void setDeployables(List<Deployable> deployables) {
//		this.deployables = deployables;
//	}
//}
