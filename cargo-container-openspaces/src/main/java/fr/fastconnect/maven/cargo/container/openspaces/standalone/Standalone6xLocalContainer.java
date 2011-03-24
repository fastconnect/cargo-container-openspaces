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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.cargo.container.ContainerCapability;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.deployable.Deployable;
import org.codehaus.cargo.container.deployable.DeployableType;
import org.codehaus.cargo.util.CargoException;

import fr.fastconnect.maven.cargo.container.openspaces.ProcessingUnitDeployable;

/**
 * Creates a standalone processing unit container using a deployable as parameter.
 * Configured deployable should be single.
 * @see org.openspaces.pu.container.standalone.StandaloneProcessingUnitContainer
 * @see http://www.gigaspaces.com/wiki/display/OLH/Open+Spaces+Standalone+Processing+Unit+Container
 */
public class Standalone6xLocalContainer {
    private final static String FILE_SEPARATOR = System.getProperty("file.separator");
    
    private ClassLoader classLoader;
    private final LocalConfiguration configuration;
    private Object container;
    
    public Standalone6xLocalContainer(final LocalConfiguration configuration) {
        this.configuration = configuration;
    }

    public void setClassLoader(final ClassLoader classLoader) {
        this.classLoader = classLoader;
    }
    
    private LocalConfiguration getConfiguration() {
        return this.configuration;
    }
    
    public ContainerCapability getCapability() {
        return new ContainerCapability() {

            public boolean supportsDeployableType(final DeployableType type) {
                return false;
            }
            
        };
    }
    
    /**
     * Creates a StandaloneProcessingUnitContainer with correct arguments.
     * See StandaloneProcessingUnitContainer#main method for more details.
     * @throws Exception test
     */
    public void start() throws Exception {
        if (getConfiguration().getDeployables().size() != 1) {
            throw new CargoException("Only supports a single "+Deployable.class.getSimpleName());
        }
        
        final ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.classLoader);

            if (System.getProperty("java.security.policy") == null) {
                Class.forName("com.j_spaces.kernel.SecurityPolicyLoader", true, this.classLoader).getMethod("loadPolicy", String.class).invoke(null, "policy/policy.all");
            }
            Class.forName("com.gigaspaces.logger.GSLogConfigLoader", true, this.classLoader).getMethod("getLoader").invoke(null);
            final Class<?> containerProviderClass = Class.forName("org.openspaces.pu.container.standalone.StandaloneProcessingUnitContainerProvider", true, this.classLoader);
            
            final ProcessingUnitDeployable deployable = (ProcessingUnitDeployable) getConfiguration().getDeployables().get(0);
            final String location;
            if (deployable.getFile().endsWith(".jar")) {
                //Only works if compiled classes location is the default one
                location = deployable.getFile().substring(0, deployable.getFile().lastIndexOf(Standalone6xLocalContainer.FILE_SEPARATOR)+1)+"classes";
            } else if (deployable.getFile().endsWith(".pu")) {
                location = deployable.getFile().substring(0, deployable.getFile().lastIndexOf(".pu"));
            } else {
                //location has been explicitely set, use it
                location = deployable.getFile();
            }
            
            final Object containerProvider = containerProviderClass.getConstructor(String.class).newInstance(location);
            
            final List<String> arguments = new ArrayList<String>(4); 
            if (deployable.getCluster() != null && !"".equals(deployable.getCluster())) {
                arguments.add("-cluster");
                arguments.add(deployable.getCluster());
            }
            if (deployable.getProperties() != null && !"".equals(deployable.getProperties())) {
                arguments.add("-properties");
                arguments.add(deployable.getProperties());
            }
                
            final String[] args = arguments.toArray(new String[arguments.size()]);
            final Object params = Class.forName("org.openspaces.pu.container.support.CommandLineParser", true, this.classLoader).getMethod("parse", String[].class).invoke(null, (Object) args);
            
            final Class<?> commandLineParameterArrayClass = Array.newInstance(Class.forName("org.openspaces.pu.container.support.CommandLineParser$Parameter", true, this.classLoader), new int[]{0}).getClass();
            
            containerProviderClass.getMethod("setBeanLevelProperties",
            		Class.forName("org.openspaces.core.properties.BeanLevelProperties", true,
            				this.classLoader)).invoke(containerProvider,
            						Class.forName("org.openspaces.pu.container.support.BeanLevelPropertiesParser", true, this.classLoader).getMethod("parse", commandLineParameterArrayClass).invoke(null, params));
            containerProviderClass.getMethod("setClusterInfo",
            		Class.forName("org.openspaces.core.cluster.ClusterInfo", true,
            				this.classLoader)).invoke(containerProvider,
            						Class.forName("org.openspaces.pu.container.support.ClusterInfoParser", true, this.classLoader).getMethod("parse", commandLineParameterArrayClass).invoke(null, params));
            
            Class.forName("org.openspaces.pu.container.support.ConfigLocationParser", true, this.classLoader).getMethod("parse", 
                    Class.forName("org.openspaces.pu.container.spi.ApplicationContextProcessingUnitContainerProvider", true, this.classLoader), commandLineParameterArrayClass).invoke(null, containerProvider, params);
            
            this.container = containerProviderClass.getMethod("createContainer").invoke(containerProvider);
            
            final Object applicationContext = this.container.getClass().getMethod("getApplicationContext").invoke(this.container);
            applicationContext.getClass().getMethod("registerShutdownHook").invoke(applicationContext);
        } finally {
            Thread.currentThread().setContextClassLoader(currentClassLoader);
        }
    }

    public void waitForCompletion(final boolean waitForStarting) throws InterruptedException {
      //standalone container deploys synchronously deployable 
    }
    
    /**
     * Calls StandaloneProcessingUnitContainer#close method.
     * @throws Exception exception
     */
    public void stop() throws Exception {
        this.container.getClass().getMethod("close").invoke(this.container);
    }

}
