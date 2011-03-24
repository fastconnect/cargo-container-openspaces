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

package fr.fastconnect.cargo.container.openspaces.deployable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.cargo.container.deployable.Deployable;
import org.codehaus.cargo.container.deployable.DeployableException;
import org.codehaus.cargo.container.deployable.DeployableType;
import org.codehaus.cargo.container.spi.deployable.AbstractDeployable;
import org.openspaces.admin.Admin;
import org.openspaces.admin.pu.DeploymentStatus;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;

import fr.fastconnect.cargo.container.openspaces.OpenSpacesContainerHelper;

/**
 * {@link Deployable} that deploys a processing unit on a grid.
 * 
 * @author luc boutier
 */
public class ProcessingUnitDeployable extends AbstractDeployable {
	private static final Log LOGGER = LogFactory.getLog(ProcessingUnitDeployable.class);

	private static final long PU_DEPLOYED_WAIT = 1000;
	private static final long DEFAULT_DEPLOYMENT_TIMEOUT = 120000;
	private static final String CONTEXT_PROPERTY_SEPARATOR = "\\|";
	private static final String VALUE_KEY_SEPARATOR = "(^[^=]*)=(.*)";

	/** properties to set for deployment. */
	private long deploymentTimeout = ProcessingUnitDeployable.DEFAULT_DEPLOYMENT_TIMEOUT;
	/** the processing unit spring context properties. */
	private String contextProperties;
	/** path to a properties file to load and set as context properties for the pu spring's context. */
	private String contextPropertiesFile;
	/** name that we want to give to the processing unit to deploy. */
	private String overrideName;
	/** cluster schema to use for deployment. */
	private String clusterSchema = null;
	/** GigaSpaces max per machine parameter. */
	private int maxPerMachine = -1;
	private int maxPerVM = -1;
	private int numberOfBackup = -1;
	private int numberOfInstances = -1;
	private boolean secured = false;
	private String userName = null;
	private String userPassword = null;
	private String provisionUser = null;
	private String provisionPassword = null;
	private String[] zones = null;
	private Map<String, Integer> maxInstancesPerZone = null;
	private String slaLocation;

	/** Admin API instance. */
	private Admin admin;

	/** LookupLocators from the container. */
	private String lookupLocators;
	/** LookupGroups from the container. */
	private String lookupGroups;

	public ProcessingUnitDeployable(final String file) {
		super(file);
	}

	private Admin getAdmin() {
		if (this.admin == null) {
			this.admin = OpenSpacesContainerHelper.getAdmin(this.lookupLocators, this.lookupGroups, this.provisionUser,
					this.provisionPassword);
		}
		return this.admin;
	}

	/**
	 * Deploy the processing unit defined by this {@link Deployable}
	 * 
	 * @param lookupLocators
	 *            The lookup locators to connect to the admin API.
	 * @param lookupGroups
	 *            The lookup groups to connect to the admin API.
	 */
	public void deploy(String lookupLocators, String lookupGroups) {
		this.lookupLocators = lookupLocators;
		this.lookupGroups = lookupGroups;
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("deploying <" + getFile() + ">");
		}

		ProcessingUnitDeployment pud = new ProcessingUnitDeployment(getFile());
		injectDeployablePropertiesToProcessingUnit(this, pud);

		final ProcessingUnit processingUnit = getAdmin().getGridServiceManagers().waitForAtLeastOne().deploy(pud);

		// if (!parallelDeployment) {
		// wait for the processing unit to be deployed on the grid.
		waitForStatus(processingUnit, OpenSpacesContainerHelper.START_WAIT_TIMEOUT, DeploymentStatus.INTACT);
		// }
		// return processingUnit;
	}

	/**
	 * Parse the properties from the Processing Unit Deployable and inject them in the OpenSpaces grid
	 * ProcessingUnitDeployment.
	 * 
	 * @param processingUnitDeployable
	 *            The ProcessingUnitDeployable given by the user.
	 * @param pud
	 *            The ProcessingUnitDeployment we have to configure.
	 */
	private void injectDeployablePropertiesToProcessingUnit(ProcessingUnitDeployable processingUnitDeployable,
			ProcessingUnitDeployment pud) {
		for (final Map.Entry<Object, Object> contextProperty : processingUnitDeployable.getParsedContextProperties()
				.entrySet()) {
			final String key = (String) contextProperty.getKey();
			final String value = (String) contextProperty.getValue();

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Set value <" + value + "> for key <" + key + ">");
			}
			pud.setContextProperty(key, value);
		}
		if (processingUnitDeployable.getZones() != null) {
			for (String zone : processingUnitDeployable.getZones()) {
				pud.addZone(zone);
			}
		}
		if (processingUnitDeployable.getMaxInstancesPerZone() != null) {
			for (Entry<String, Integer> maxPerZone : processingUnitDeployable.getMaxInstancesPerZone().entrySet()) {
				pud.maxInstancesPerZone(maxPerZone.getKey(), maxPerZone.getValue());
			}
		}
		pud.slaLocation(processingUnitDeployable.getSlaLocation());
		pud.name(processingUnitDeployable.getOverrideName());
		if (processingUnitDeployable.isSecured()) {
			pud.userDetails(processingUnitDeployable.getUserName(), processingUnitDeployable.getUserPassword());
		}
		pud.secured(processingUnitDeployable.isSecured());
		if (processingUnitDeployable.getClusterSchema() != null) {
			pud.clusterSchema(processingUnitDeployable.getClusterSchema());
		}
		if (processingUnitDeployable.getMaxPerMachine() != -1) {
			pud.maxInstancesPerMachine(processingUnitDeployable.getMaxPerMachine());
		}
		if (processingUnitDeployable.getMaxPerVM() != -1) {
			pud.maxInstancesPerVM(processingUnitDeployable.getMaxPerVM());
		}
		if (processingUnitDeployable.getNumberOfBackup() != -1) {
			pud.numberOfBackups(processingUnitDeployable.getNumberOfBackup());
		}
		if (processingUnitDeployable.getNumberOfInstances() != -1) {
			pud.numberOfInstances(processingUnitDeployable.getNumberOfInstances());
		}
	}

	/**
	 * Undeploy this processing unit from the grid.
	 * 
	 * @param lookupLocators
	 *            The lookup locators to connect to the admin API.
	 * @param lookupGroups
	 *            The lookup groups to connect to the admin API.
	 */
	public void undeploy(String lookupLocators, String lookupGroups) {
		this.lookupLocators = lookupLocators;
		this.lookupGroups = lookupGroups;

		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("Undeploying <" + toString() + ">");
		}

		try {
			String puName = getName();
			if (getOverrideName() != null) {
				puName = getOverrideName();
			}
			final ProcessingUnit pu = getAdmin().getProcessingUnits().waitFor(puName, getDeploymentTimeout(),
					TimeUnit.MILLISECONDS);
			if (pu == null) {
				throw new DeployableException("Failed to access pu <" + puName + "> after <" + getDeploymentTimeout()
						+ "> ms");
			}

			// undeploy is a blocking operation
			pu.undeploy();

			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("Successfully undeployed <" + toString() + ">");
			}
		} finally {
			getAdmin().close();
		}
	}

	// @Override
	// public void deploy(@SuppressWarnings("rawtypes") List deployables) {
	// if (this.admin == null) {
	// this.admin = OpenSpacesContainerHelper.getAdmin(locators, groups);
	// }
	//
	// try {
	// @SuppressWarnings("unchecked")
	// List<Deployable> dList = deployables;
	// List<ProcessingUnit> pus = new ArrayList<ProcessingUnit>(dList.size());
	//
	// for (Deployable deployable : dList) {
	// pus.add(deployProcessingUnit(this.admin, deployable));
	// }
	//
	// if (parallelDeployment) {
	// for (ProcessingUnit pu : pus) {
	// ProcessingUnitDeployer.waitForStatus(pu, OpenSpacesContainerHelper.START_WAIT_TIMEOUT,
	// DeploymentStatus.INTACT);
	// }
	// }
	// } finally {
	// admin.close();
	// }
	// }

	public static void waitForStatus(final ProcessingUnit processingUnit, final long timeout,
			final DeploymentStatus status) {
		long startLoop = System.currentTimeMillis();
		while (!processingUnit.getStatus().equals(status)) {
			try {
				Thread.sleep(ProcessingUnitDeployable.PU_DEPLOYED_WAIT);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
			if ((System.currentTimeMillis() - startLoop) > timeout) {
				if (LOGGER.isWarnEnabled()) {
					LOGGER.warn("Deployment status was <" + processingUnit.getStatus() + "> after <" + timeout + "> ms");
				}
				break;
			}
		}

		if (DeploymentStatus.BROKEN.equals(processingUnit.getStatus())) {
			throw new DeployableException("Failed to deploy <" + processingUnit.getName() + ">");
		}
	}

	public void setZones(String zones) {
		this.zones = zones.split(",");
	}

	public String[] getZones() {
		return zones;
	}

	public void setMaxInstancesPerZones(String maxInstancesPerZones) {
		String[] maxPerZones = maxInstancesPerZones.split(",");
		this.maxInstancesPerZone = new HashMap<String, Integer>();
		for (String maxPerZone : maxPerZones) {
			String[] keyVal = maxPerZone.split("=");
			if (keyVal.length == 2) {
				this.maxInstancesPerZone.put(keyVal[0].trim(), Integer.valueOf(keyVal[1].trim()));
			} else {
				LOGGER.warn("Invalid maxInstancePerZones property <"
						+ maxInstancesPerZones
						+ "> this must be a list of key=value couple separated by comma: \"zoneName=2,otherZoneName=3\"");
			}
		}
	}

	public Map<String, Integer> getMaxInstancesPerZone() {
		return maxInstancesPerZone;
	}

	public void setSlaLocation(String slaLocation) {
		this.slaLocation = slaLocation;
	}

	public String getSlaLocation() {
		return slaLocation;
	}

	public String getOverrideName() {
		return overrideName;
	}

	public void setOverrideName(String overrideName) {
		this.overrideName = overrideName;
	}

	public void setSecured(String secured) {
		this.secured = Boolean.valueOf(secured);
	}

	public boolean isSecured() {
		return secured;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserPassword(String userPassword) {
		this.userPassword = userPassword;
	}

	public String getUserPassword() {
		return userPassword;
	}

	public long getDeploymentTimeout() {
		return this.deploymentTimeout;
	}

	public void setDeploymentTimeout(final String deploymentTimeout) {
		this.deploymentTimeout = Long.valueOf(deploymentTimeout);
	}

	public String getContextProperties() {
		return this.contextProperties;
	}

	public void setContextProperties(final String properties) {
		this.contextProperties = properties;
	}

	public String getContextPropertiesFile() {
		return contextPropertiesFile;
	}

	public void setContextPropertiesFile(String contextPropertiesFile) {
		this.contextPropertiesFile = contextPropertiesFile;
	}

	public void setClusterSchema(String clusterSchema) {
		this.clusterSchema = clusterSchema;
	}

	public String getClusterSchema() {
		return clusterSchema;
	}

	public void setMaxPerMachine(String maxPerMachine) {
		this.maxPerMachine = Integer.valueOf(maxPerMachine);
	}

	public int getMaxPerMachine() {
		return maxPerMachine;
	}

	public void setMaxPerVM(String maxPerVM) {
		this.maxPerVM = Integer.valueOf(maxPerVM);
	}

	public int getMaxPerVM() {
		return maxPerVM;
	}

	public void setNumberOfBackup(String numberOfBackup) {
		this.numberOfBackup = Integer.valueOf(numberOfBackup);
	}

	public int getNumberOfBackup() {
		return numberOfBackup;
	}

	public void setNumberOfInstances(String numberOfInstances) {
		this.numberOfInstances = Integer.valueOf(numberOfInstances);
	}

	public int getNumberOfInstances() {
		return numberOfInstances;
	}

	public Properties getParsedContextProperties() {
		final Properties properties = new Properties();
		if (this.contextPropertiesFile != null) {
			try {
				properties.load(new FileInputStream(new File(this.contextPropertiesFile)));
			} catch (FileNotFoundException e) {
				LOGGER.error("Unable to load properties from file.", e);
			} catch (IOException e) {
				LOGGER.error("Unable to load properties from file.", e);
			}
		}
		if (this.contextProperties != null) {
			final String[] keyValues = this.contextProperties
					.split(ProcessingUnitDeployable.CONTEXT_PROPERTY_SEPARATOR);
			for (final String keyValue : keyValues) {
				final Matcher matcher = Pattern.compile(ProcessingUnitDeployable.VALUE_KEY_SEPARATOR).matcher(keyValue);
				if (matcher.matches() && matcher.groupCount() == 2) {
					final String key = matcher.group(1);
					final String value = matcher.group(2);
					properties.put(key, value.toString());
				} else {
					throw new IllegalArgumentException("Cannot parse <" + keyValue
							+ ">; key/value must be separated by " + ProcessingUnitDeployable.VALUE_KEY_SEPARATOR);
				}
			}
		}
		return properties;
	}

	public DeployableType getType() {
		return ProcessingUnitDeployable.getDeployableType();
	}

	public static DeployableType getDeployableType() {
		return DeployableType.toType("pu");
	}

	public String getName() {
		int puNameStartPos = 0;
		int slashPos = getFile().lastIndexOf("/");
		int backslashPos = getFile().lastIndexOf("\\");
		if (backslashPos > slashPos) {
			puNameStartPos = backslashPos + 1;
		} else {
			puNameStartPos = slashPos + 1;
		}
		String puName;
		if (getFile().endsWith(".jar")) {
			puName = getFile().substring(puNameStartPos, getFile().lastIndexOf(".jar"));
		} else {
			puName = getFile().substring(puNameStartPos, getFile().length());
		}
		return puName;
	}

	@Override
	public String toString() {
		return "ProcessingUnit <" + getName() + ">";
	}

	public String getProvisionPassword() {
		return provisionPassword;
	}

	public void setProvisionPassword(String provisionPassword) {
		this.provisionPassword = provisionPassword;
	}

	public String getProvisionUser() {
		return provisionUser;
	}

	public void setProvisionUser(String provisionUser) {
		this.provisionUser = provisionUser;
	}
}