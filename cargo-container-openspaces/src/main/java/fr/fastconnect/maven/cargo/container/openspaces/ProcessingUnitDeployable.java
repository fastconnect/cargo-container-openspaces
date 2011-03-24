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

package fr.fastconnect.maven.cargo.container.openspaces;

import org.codehaus.cargo.container.deployable.DeployableType;
import org.codehaus.cargo.container.spi.deployable.AbstractDeployable;

/**
 *Encapsulates {@link org.codehaus.cargo.container.deployable.Deployable} configuration for a processing Unit Instance.
 *Common to all {@link org.openspaces.pu.container.ProcessingUnitContainer}.
 *<br />
 *Allows to provide:
 *<ul>
 *  <li>cluster parameter</li>
 *  <li>properties parameter</li>
 *</ul>
 *
 *@see http://www.gigaspaces.com/wiki/display/OLH/Open+Spaces+Processing+Unit+Containers
 */
public class ProcessingUnitDeployable extends AbstractDeployable {

    private String cluster;
    private String properties;
    
    public ProcessingUnitDeployable(final String file) {
        super(file);
    }
    
    public String getCluster() {
        return this.cluster;
    }
    
    public void setCluster(final String cluster) {
        this.cluster = cluster;
    }
    
    public String getProperties() {
        return this.properties;
    }
    
    public void setProperties(final String properties) {
        this.properties = properties;
    }
    
    public DeployableType getType() {
        return DeployableType.toType("pu");
    }
    
}
