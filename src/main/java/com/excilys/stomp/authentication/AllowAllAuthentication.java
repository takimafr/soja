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
public class AllowAllAuthentication implements Authentication {
	
	public static final Authentication INSTANCE = new AllowAllAuthentication();

	public String connect(String username, String password) throws LoginException {
		return "token-" + username;
	}

	public boolean canSend(String token, String topic) {
		return true;
	}

	public boolean canSubscribe(String token, String topic) {
		return true;
	}

}
