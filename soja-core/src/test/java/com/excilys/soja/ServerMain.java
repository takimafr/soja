/**
###############################################################################
# Contributors:
#     Damien VILLENEUVE - initial API and implementation
###############################################################################
 */
package com.excilys.soja;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.excilys.soja.StompServer;
import com.excilys.soja.authentication.Authentication;

/**
 * @author dvilleneuve
 * 
 */
public class ServerMain {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServerMain.class);

	public static void main(String[] args) {
		final StompServer server = new StompServer(61626, Authentication.ALLOW_ALL_INSTANCE);
		server.start();

		// Add a hook on the user interupt event
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				LOGGER.debug("User intercept event catched.");
				server.stop();
			}
		});
	}

}
