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

import java.util.HashMap;
import java.util.Map;

import org.codehaus.cargo.container.LocalContainer;
import org.codehaus.cargo.container.configuration.ConfigurationCapability;
import org.codehaus.cargo.container.spi.configuration.AbstractStandaloneLocalConfiguration;
import org.codehaus.cargo.container.spi.configuration.AbstractStandaloneLocalConfigurationCapability;

/**
 * Configuration for Cargo OpenSpaces 7.x container
 * @author luc boutier
 */
public class OpensSpaces7xInstalledLocalConfiguration extends AbstractStandaloneLocalConfiguration {

    public static final String CARGO_LOOKUP_GROUPS = "com.gs.jini_lus.groups";
    public static final String CARGO_LOOKUP_LOCATORS = "com.gs.jini_lus.locators";
    public static final String CARGO_LICENSEKEY = "com.gs.licensekey";
    public static final String CARGO_AGENTCONFIGURATION = "agent.grid.configuration";
    
    public OpensSpaces7xInstalledLocalConfiguration(final String home) {
        super(home);
    }

    @Override
    protected void doConfigure(final LocalContainer container) throws Exception {
    }

    public ConfigurationCapability getCapability() {
        return new AbstractStandaloneLocalConfigurationCapability() {

            @Override
            protected Map<String, Boolean> getPropertySupportMap() {
                HashMap<String, Boolean> propertySupportMap = new HashMap<String, Boolean>();
                propertySupportMap.put(CARGO_LOOKUP_GROUPS, true);
                propertySupportMap.put(CARGO_LOOKUP_LOCATORS, true);
                propertySupportMap.put(CARGO_LICENSEKEY, true);
                propertySupportMap.put(CARGO_AGENTCONFIGURATION, true);
                return propertySupportMap;
            }
            
        };
    }
}