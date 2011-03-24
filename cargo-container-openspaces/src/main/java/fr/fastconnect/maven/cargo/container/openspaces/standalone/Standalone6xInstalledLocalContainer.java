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

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.Set;

import org.apache.tools.ant.taskdefs.Java;
import org.codehaus.cargo.container.ContainerCapability;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.spi.AbstractInstalledLocalContainer;

/**
 * Implementation of {@link org.codehaus.cargo.container.InstalledLocalContainer} using {@link Standalone6xLocalContainer} 
 * with a {@link ClassLoader} constructed based on {@link InstalledLocalContainer#getHome()} value.
 */
public class Standalone6xInstalledLocalContainer extends AbstractInstalledLocalContainer {
    
    private final Standalone6xLocalContainer standalone6xLocalContainer;
    
    public Standalone6xInstalledLocalContainer(final LocalConfiguration configuration) {
        super(configuration);
        this.standalone6xLocalContainer = new Standalone6xLocalContainer(configuration);
    }
    
    public String getId() {
        return "standalone-installed-puc-6x";
    }

    public String getName() {
        return "Standalone Installed ProcessingUnitContainer 6.x";
    }
    
    public ContainerCapability getCapability() {
        return this.standalone6xLocalContainer.getCapability();
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected void doStart(final Java java) throws Exception {
        final URL[] urls = {
            new File(getHome()+"/lib/spring/spring.jar").toURL(),
            new File(getHome()+"/lib/JSpaces.jar").toURL(), 
            new File(getHome()+"/lib/openspaces/openspaces.jar").toURL(),
            new File(getHome()+"/lib/common/commons-logging.jar").toURL()
        };
        
        //manually set system properties
        if (getSystemProperties() != null) {
            for (final Map.Entry<String, String> entry : (Set<Map.Entry<String, String>>) getSystemProperties().entrySet()) {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        }
        AccessController.doPrivileged(new PrivilegedAction<Void>() {

            public Void run() {
                Standalone6xInstalledLocalContainer.this.standalone6xLocalContainer.setClassLoader(new URLClassLoader(urls));
                return null;
            }
            
        });
        this.standalone6xLocalContainer.start();
    }
    
    @Override
    protected void waitForCompletion(final boolean waitForStarting) throws InterruptedException {
        this.standalone6xLocalContainer.waitForCompletion(waitForStarting);  
    }

    @Override
    protected void doStop(final Java java) throws Exception {
        this.standalone6xLocalContainer.stop();
    }

}
