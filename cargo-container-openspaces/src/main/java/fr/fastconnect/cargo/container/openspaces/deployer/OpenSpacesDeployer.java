package fr.fastconnect.cargo.container.openspaces.deployer;

import org.codehaus.cargo.container.RemoteContainer;
import org.codehaus.cargo.container.deployable.Deployable;
import org.codehaus.cargo.container.deployer.DeployerType;
import org.codehaus.cargo.container.spi.deployer.AbstractDeployer;

import fr.fastconnect.cargo.container.openspaces.deployable.GridDeployable;
import fr.fastconnect.cargo.container.openspaces.deployable.ProcessingUnitDeployable;
import fr.fastconnect.cargo.container.openspaces.remote.OpenSpaces7xRemoteContainer;

public class OpenSpacesDeployer extends AbstractDeployer {
	private OpenSpaces7xRemoteContainer container;

	public DeployerType getType() {
		return DeployerType.REMOTE;
	}

	public OpenSpacesDeployer(RemoteContainer container) {
		this.container = (OpenSpaces7xRemoteContainer) container;
	}

	@Override
	public void deploy(Deployable deployable) {
		if (deployable instanceof GridDeployable) {
			// don't do anything as grid deployables are deployed using deployer-start mojo.
		} else if (deployable instanceof ProcessingUnitDeployable) {
			((ProcessingUnitDeployable) deployable).deploy(this.container.getLookupLocators(),
					this.container.getLookupGroups());
		} else {
			// TODO throw exception
		}
	}

	@Override
	public void start(Deployable deployable) {
		if (deployable instanceof GridDeployable) {
			try {
				((GridDeployable) deployable).deploy(this.container.getLookupLocators(),
						this.container.getLookupGroups());
			} catch (InterruptedException e) {
				throw new RuntimeException("Error during deployment of the grid", e);
			}
		} else if (deployable instanceof ProcessingUnitDeployable) {
			// don't do anything as pu deployables are deployed using deploy mojo.
		} else {
			// TODO throw exception
		}
	}

	@Override
	public void stop(Deployable deployable) {
		if (deployable instanceof GridDeployable) {
			((GridDeployable) deployable)
					.undeploy(this.container.getLookupLocators(), this.container.getLookupGroups());
		} else if (deployable instanceof ProcessingUnitDeployable) {
			// don't do anything as pu deployables are undeployed using undeploy mojo.
		} else {
			// TODO throw exception
		}
	}

	@Override
	public void undeploy(Deployable deployable) {
		if (deployable instanceof GridDeployable) {
			// don't do anything as grid deployables are un deployed using deployer-stop mojo.
		} else if (deployable instanceof ProcessingUnitDeployable) {
			((ProcessingUnitDeployable) deployable).undeploy(this.container.getLookupLocators(),
					this.container.getLookupGroups());
		} else {
			// TODO throw exception
		}
	}
}
