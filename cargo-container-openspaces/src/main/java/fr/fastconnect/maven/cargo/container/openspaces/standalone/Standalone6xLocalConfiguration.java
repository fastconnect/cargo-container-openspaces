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

package fr.fastconnect.maven.cargo.container.openspaces.standalone;

import java.util.Map;

import org.codehaus.cargo.container.LocalContainer;
import org.codehaus.cargo.container.configuration.ConfigurationCapability;
import org.codehaus.cargo.container.spi.configuration.AbstractStandaloneLocalConfiguration;
import org.codehaus.cargo.container.spi.configuration.AbstractStandaloneLocalConfigurationCapability;

/**
 * Configuration for Standalone Processing Unit Container.
 * No particular features added.
 */
public class Standalone6xLocalConfiguration extends AbstractStandaloneLocalConfiguration {
    
    public Standalone6xLocalConfiguration(final String home) {
        super(home);
    }

    @Override
    protected void doConfigure(final LocalContainer container) throws Exception {
    }

    public ConfigurationCapability getCapability() {
        return new AbstractStandaloneLocalConfigurationCapability() {

            @Override
            protected Map<?, ?> getPropertySupportMap() {
                return null;
            }
            
        };
    }

}
