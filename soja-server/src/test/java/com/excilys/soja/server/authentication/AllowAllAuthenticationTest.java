package com.excilys.soja.server.authentication;

import static junit.framework.Assert.assertNotNull;

import org.junit.Test;

public class AllowAllAuthenticationTest extends AuthenticationTest {

	public AllowAllAuthenticationTest() {
		authentication = new AllowAllAuthentication();
	}

	@Test
	public void testConnect() throws Exception {
		assertNotNull(authentication.connect("username", "password"));
	}

	@Test
	public void testConnect_null() throws Exception {
		assertNotNull(authentication.connect(null, null));
	}

}
