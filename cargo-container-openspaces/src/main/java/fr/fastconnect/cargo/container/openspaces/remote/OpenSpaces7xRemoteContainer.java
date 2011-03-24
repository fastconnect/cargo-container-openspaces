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

package fr.fastconnect.cargo.container.openspaces.remote;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.cargo.container.ContainerCapability;
import org.codehaus.cargo.container.ContainerType;
import org.codehaus.cargo.container.configuration.RuntimeConfiguration;
import org.codehaus.cargo.container.deployable.DeployableType;
import org.codehaus.cargo.container.spi.AbstractRemoteContainer;

import fr.fastconnect.cargo.container.openspaces.deployable.GridDeployable;
import fr.fastconnect.cargo.container.openspaces.deployable.ProcessingUnitDeployable;

/**
 * Start a GigaSpaces grid (GSC/GSM) according to the given configuration. This is using the Grid Service Agent
 * capabilities.
 * 
 * @author luc boutier
 */
public class OpenSpaces7xRemoteContainer extends AbstractRemoteContainer {
	private final static Log logger = LogFactory.getLog(OpenSpaces7xRemoteContainer.class);

	private static final String ID = "openspaces-remote";
	private static final String NAME = "OpenSpaces Remote Container";

	private String lookupGroups;
	private String lookupLocators;

	public OpenSpaces7xRemoteContainer(final RuntimeConfiguration configuration) {
		super(configuration);
		if (configuration instanceof OpenSpaces7xRemoteConfiguration) {
			this.lookupGroups = ((OpenSpaces7xRemoteConfiguration) configuration).getLookupGroups();
			this.lookupLocators = ((OpenSpaces7xRemoteConfiguration) configuration).getLookupLocators();
		}
	}

	public String getLookupGroups() {
		return lookupGroups;
	}

	public String getLookupLocators() {
		return lookupLocators;
	}

	public ContainerCapability getCapability() {
		logger.info("Get capability");
		return new ContainerCapability() {
			public boolean supportsDeployableType(final DeployableType type) {
				if (type.equals(ProcessingUnitDeployable.getDeployableType())) {
					return true;
				}
				if (type.equals(GridDeployable.getDeployableType())) {
					return true;
				}
				return false;
			}
		};
	}

	@Override
	public ContainerType getType() {
		return ContainerType.REMOTE;
	}

	public String getId() {
		return ID;
	}

	public String getName() {
		return NAME;
	}
}
