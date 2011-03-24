package fr.fastconnect.cargo.container.openspaces;

import org.junit.Test;
import org.openspaces.core.space.UrlSpaceConfigurer;

public class BasicSpaceOperationsIT {

	@Test
	public void testSpaceAccess() {
		new UrlSpaceConfigurer("jini://*/*/cargo-openspaces-container-it-space").lookupLocators("localhost:4166").space();
	}
}
