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
public class RefuseAllAuthentication implements Authentication {

	public static final Authentication INSTANCE = new RefuseAllAuthentication();

	public String connect(String username, String password) throws LoginException {
		throw new LoginException();
	}

	public boolean canSend(String token, String topic) {
		return false;
	}

	public boolean canSubscribe(String token, String topic) {
		return false;
	}

}
