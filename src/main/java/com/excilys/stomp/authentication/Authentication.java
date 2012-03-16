/**
###############################################################################
# Contributors:
#     Damien VILLENEUVE - initial API and implementation
###############################################################################
 */
package com.excilys.stomp.authentication;

import javax.security.auth.login.LoginException;

/**
 * @author dvilleneuve
 * 
 */
public interface Authentication {

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
