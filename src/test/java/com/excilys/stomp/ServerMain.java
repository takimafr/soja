/**
###############################################################################
# Contributors:
#     Damien VILLENEUVE - initial API and implementation
###############################################################################
 */
package com.excilys.stomp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.excilys.stomp.StompServer;

/**
 * @author dvilleneuve
 * 
 */
public class ServerMain {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServerMain.class);

	public static void main(String[] args) {
		final StompServer server = new StompServer(61626);
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
