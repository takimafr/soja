package com.excilys.soja.server.authentication;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;

import org.junit.Test;

public abstract class AuthenticationTest {

	protected Authentication authentication;

	@Test
	public void testGenerateToken() throws Exception {
		assertNotNull(authentication.generateToken("test"));
	}

	@Test
	public void testGenerateToken_null() throws Exception {
		assertNotNull(authentication.generateToken(null));
	}

	@Test
	public void testGenerateToken_same_user() throws Exception {
		String token1 = authentication.generateToken("test");
		String token2 = authentication.generateToken("test");

		assertFalse(token1.equals(token2));
	}

}
