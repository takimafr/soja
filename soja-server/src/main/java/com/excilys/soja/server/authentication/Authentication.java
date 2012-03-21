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

import javax.security.auth.login.LoginException;

/**
 * @author dvilleneuve
 * 
 */
public interface Authentication {
	
	public static final Authentication ALLOW_ALL_INSTANCE = new AllowAllAuthentication();
	public static final Authentication DENY_ALL_INSTANCE = new DenyAllAuthentication();

	/**
	 * Try to authenticate a user.
	 * 
	 * @param username
	 * @param password
	 * @return a token associated to this user. This token will be used for future authorization requests.
	 * @throws LoginException
	 *             if connection failed
	 */
	public String connect(String username, String password) throws LoginException;

	/**
	 * Get authorization to send a message on a specific topic.
	 * 
	 * @param token
	 * @param topic
	 * @return true if the user identified by the token can publish on this topic, false else
	 */
	public boolean canSend(String token, String topic);

	/**
	 * Get authorization to subscribe to a specific topic.
	 * 
	 * @param token
	 * @param topic
	 * @return true if the user identified by the token can subscribe to this topic, false else
	 */
	public boolean canSubscribe(String token, String topic);

}
