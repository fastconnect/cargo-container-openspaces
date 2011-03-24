package fr.fastconnect.cargo.container.openspaces.deployable;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.cargo.container.deployable.Deployable;
import org.codehaus.cargo.container.deployable.DeployableType;
import org.codehaus.cargo.container.spi.deployable.AbstractDeployable;
import org.openspaces.admin.Admin;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsa.GridServiceAgents;
import org.openspaces.admin.gsa.GridServiceContainerOptions;
import org.openspaces.admin.gsa.GridServiceManagerOptions;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;

import fr.fastconnect.cargo.container.openspaces.OpenSpacesContainerHelper;

/**
 * {@link Deployable} to deploy a GigaSpaces grid on machines that have an available agent.
 * 
 * @author luc boutier
 */
public class GridDeployable extends AbstractDeployable {
	/** logger. */
	private final static Log LOGGER = LogFactory.getLog(GridDeployable.class);

	/** expected gsa before deploying. */
	private int expectedGsaCount;
	/** Number of GSMs to start. */
	private int gsmCount;
	/** number of GSCs to start. */
	private int gscCount;
	/** VM options for the GSC. */
	private String gscVmOptions;
	/** grid user. */
	private String gridUser;
	/** grid password. */
	private String gridPassword;

	/** Admin API instance. */
	private Admin admin;

	/** LookupLocators from the container. */
	private String lookupLocators;
	/** LookupGroups from the container. */
	private String lookupGroups;

	public GridDeployable(String file) {
		super(file);
	}

	private Admin getAdmin() {
		if (this.admin == null) {
			this.admin = OpenSpacesContainerHelper.getAdmin(this.lookupLocators, this.lookupGroups, this.gridUser,
					this.gridPassword);
		}
		return this.admin;
	}

	public void deploy(String lookupLocators, String lookupGroups) throws InterruptedException {
		this.lookupLocators = lookupLocators;
		this.lookupGroups = lookupGroups;

		LOGGER.info("Connecting to GigaSpaces Agent...");

		// get the different agents part of the grid
		GridServiceAgents agents = getAdmin().getGridServiceAgents();
		agents.waitFor(this.expectedGsaCount, OpenSpacesContainerHelper.START_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
		// get the available agents to deploy the grid on...
		GridServiceAgent[] gsas = agents.getAgents();

		LOGGER.info("Starting grid...");
		startGrid(gsas);
		// waiting for services to be indeed started
		waitGridStart();
		LOGGER.info("Grid started successfully");
	}

	/**
	 * Now we should deploy GSMs and GSCs on the grid. Current implementation just support random deployment but we
	 * should make something smarter.
	 * 
	 * @param gsas
	 *            The agents on which to start the GSMs and GSCs
	 */
	private void startGrid(GridServiceAgent[] gsas) {
		GridServiceManagerOptions gsmopt = new GridServiceManagerOptions();
		int agentIncrement = 0;
		for (int i = 0; i < this.gsmCount; i++) {
			gsas[agentIncrement].startGridService(gsmopt);
			agentIncrement++;
			if (agentIncrement <= gsas.length) {
				agentIncrement = 0;
			}
		}

		GridServiceContainerOptions gscContainerOptions = new GridServiceContainerOptions();
		if (this.gscVmOptions != null) {
			gscContainerOptions.vmInputArgument(this.gscVmOptions);
		}
		agentIncrement = 0;
		for (int i = 0; i < this.gscCount; i++) {
			gsas[agentIncrement].startGridService(gscContainerOptions);
			agentIncrement++;
			if (agentIncrement <= gsas.length) {
				agentIncrement = 0;
			}
		}
	}

	/**
	 * Wait for the processes to be indeed started and discovered.
	 * 
	 * @throws InterruptedException
	 *             In case the grid fail to start within the defined timeout.
	 */
	private void waitGridStart() throws InterruptedException {
		// wait for grid to be started
		if (!(getAdmin().getGridServiceContainers().waitFor(gscCount, OpenSpacesContainerHelper.START_WAIT_TIMEOUT,
				TimeUnit.MILLISECONDS) && getAdmin().getGridServiceManagers().waitFor(gsmCount,
				OpenSpacesContainerHelper.START_WAIT_TIMEOUT, TimeUnit.MILLISECONDS))) {
			final int discoveredGSC = getAdmin().getGridServiceContainers().getSize();
			final int discoveredGSM = getAdmin().getGridServiceManagers().getSize();
			throw new InterruptedException("Timeout occured before Grid components are started; " + "discovered <"
					+ discoveredGSC + "> GSC, <" + discoveredGSM + "> GSM using locators <"
					+ Arrays.toString(getAdmin().getLocators()) + "> and groups <"
					+ Arrays.toString(getAdmin().getGroups()) + ">" + "but was expecting <" + this.gscCount
					+ "> GSC, <" + this.gsmCount + "> GSM");
		}
	}

	public void undeploy(String lookupLocators, String lookupGroups) {
		this.lookupLocators = lookupLocators;
		this.lookupGroups = lookupGroups;

		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("Connecting to gsAgent...");
		}
		// get the different agents part of the grid
		GridServiceAgents agents = getAdmin().getGridServiceAgents();
		agents.waitFor(this.expectedGsaCount, OpenSpacesContainerHelper.START_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);

		LOGGER.info("agent found - stopping grid");
		// wait for GSCs to be discovered
		getAdmin().getGridServiceContainers().waitFor(this.gscCount, OpenSpacesContainerHelper.START_WAIT_TIMEOUT,
				TimeUnit.MILLISECONDS);
		// wait for GSMs to be discovered
		getAdmin().getGridServiceManagers().waitFor(this.gsmCount, OpenSpacesContainerHelper.START_WAIT_TIMEOUT,
				TimeUnit.MILLISECONDS);

		// kill gscs
		LOGGER.info("agent found - stopping gscs");
		GridServiceContainer[] containers = getAdmin().getGridServiceContainers().getContainers();
		for (GridServiceContainer container : containers) {
			container.getGridServiceAgent().killByAgentId(container.getAgentId());
		}
		// kill gsms
		LOGGER.info("agent found - stopping gsms");
		GridServiceManager[] managers = getAdmin().getGridServiceManagers().getManagers();
		for (GridServiceManager manager : managers) {
			manager.getGridServiceAgent().killByAgentId(manager.getAgentId());
		}

		getAdmin().close();

		LOGGER.info("grid stopped successfully");
	}

	public DeployableType getType() {
		return ProcessingUnitDeployable.getDeployableType();
	}

	public static DeployableType getDeployableType() {
		return DeployableType.toType("OpenSpacesGrid");
	}

	public int getExpectedGsaCount() {
		return expectedGsaCount;
	}

	public void setExpectedGsaCount(String expectedGsaCount) {
		this.expectedGsaCount = Integer.valueOf(expectedGsaCount);
	}

	public int getGsmCount() {
		return gsmCount;
	}

	public void setGsmCount(String gsmCount) {
		this.gsmCount = Integer.valueOf(gsmCount);
	}

	public int getGscCount() {
		return gscCount;
	}

	public void setGscCount(String gscCount) {
		this.gscCount = Integer.valueOf(gscCount);
	}

	public String getGscVmOptions() {
		return gscVmOptions;
	}

	public void setGscVmOptions(String gscVmOptions) {
		this.gscVmOptions = gscVmOptions;
	}

	public String getGridUser() {
		return gridUser;
	}

	public void setGridUser(String gridUser) {
		this.gridUser = gridUser;
	}

	public String getGridPassword() {
		return gridPassword;
	}

	public void setGridPassword(String gridPassword) {
		this.gridPassword = gridPassword;
	}
}
