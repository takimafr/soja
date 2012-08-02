/**
 * Copyright 2010-2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.excilys.soja.server.authentication;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.security.auth.login.LoginException;

/**
 * @author dvilleneuve
 * 
 */
public abstract class Authentication {

	public static final Authentication ALLOW_ALL_INSTANCE = new AllowAllAuthentication();
	public static final Authentication DENY_ALL_INSTANCE = new DenyAllAuthentication();

	private static MessageDigest md5Digest;
	private static int counter = 0;

	static {

		try {
			md5Digest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Try to authenticate a user. If authentication failed a LoginException will be thrown, else a <b>UNIQUE</b>
	 * session token will be returned. This token will be used internally and will be sent in the CONNECTED frame.
	 * 
	 * @param username
	 * @param password
	 * @return a token associated to this user. This token will be used for future authorization requests.
	 * @throws LoginException
	 *             if connection failed
	 */
	public abstract String connect(String username, String password) throws LoginException;

	/**
	 * Get authorization to send a message on a specific topic.
	 * 
	 * @param token
	 * @param topic
	 * @return true if the user identified by the token can publish on this topic, false else
	 */
	public abstract boolean canSend(String token, String topic);

	/**
	 * Get authorization to subscribe to a specific topic.
	 * 
	 * @param token
	 * @param topic
	 * @return true if the user identified by the token can subscribe to this topic, false else
	 */
	public abstract boolean canSubscribe(String token, String topic);

	public String generateToken(String username) {
		String plainToken = "token-" + (++counter) + "-" + username;
		byte[] digestedToken = md5Digest.digest(plainToken.getBytes());
		return convertHexToString(digestedToken);
	}

	private String convertHexToString(byte[] bytes) {
		StringBuilder hexString = new StringBuilder();
		for (int i = 0; i < bytes.length; i++) {
			String hex = Integer.toHexString(0xff & bytes[i]);
			if (hex.length() == 1)
				hexString.append('0');
			hexString.append(hex);
		}
		return hexString.toString();
	}

}
