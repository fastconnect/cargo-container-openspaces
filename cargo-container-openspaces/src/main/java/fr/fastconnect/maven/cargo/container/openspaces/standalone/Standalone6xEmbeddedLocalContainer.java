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

import org.codehaus.cargo.container.ContainerCapability;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.spi.AbstractEmbeddedLocalContainer;

/**
 * Implementation of {@link org.codehaus.cargo.container.EmbeddedLocalContainer} using {@link Standalone6xLocalContainer} 
 * with {@link org.codehaus.cargo.container.EmbeddedLocalContainer#getClassLoader()} as {@link ClassLoader}.
 */
public class Standalone6xEmbeddedLocalContainer extends AbstractEmbeddedLocalContainer {
    
    private final Standalone6xLocalContainer standalone6xLocalContainer;
    
    public Standalone6xEmbeddedLocalContainer(final LocalConfiguration configuration) {
        super(configuration);
        this.standalone6xLocalContainer = new Standalone6xLocalContainer(configuration);
    }

    public String getId() {
        return "standalone-embedded-puc-6x";
    }

    public String getName() {
        return "Standalone Embedded ProcessingUnitContainer 6.x";
    }
    
    public ContainerCapability getCapability() {
        return this.standalone6xLocalContainer.getCapability();
    }
    
    @Override
    protected void doStart() throws Exception {
        this.standalone6xLocalContainer.setClassLoader(getClassLoader());
        this.standalone6xLocalContainer.start();
    }

    @Override
    protected void waitForCompletion(final boolean waitForStarting) throws InterruptedException {
        this.standalone6xLocalContainer.waitForCompletion(waitForStarting);  
    }
    
    @Override
    protected void doStop() throws Exception {
        standalone6xLocalContainer.stop();
    }

}
