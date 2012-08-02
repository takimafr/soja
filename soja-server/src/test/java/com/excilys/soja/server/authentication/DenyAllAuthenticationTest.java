package com.excilys.soja.server.authentication;

import javax.security.auth.login.LoginException;

import org.junit.Test;

public class DenyAllAuthenticationTest extends AuthenticationTest {

	public DenyAllAuthenticationTest() {
		authentication = new DenyAllAuthentication();
	}

	@Test(expected = LoginException.class)
	public void testConnect() throws Exception {
		authentication.connect("username", "password");
	}

	@Test(expected = LoginException.class)
	public void testConnect_null() throws Exception {
		authentication.connect(null, null);
	}
}
