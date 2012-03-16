/**
###############################################################################
# Contributors:
#     Damien VILLENEUVE - initial API and implementation
###############################################################################
 */
package com.excilys.stomp;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.excilys.stomp.events.StompClientListener;

/**
 * @author dvilleneuve
 * 
 */
public class ClientMain implements StompClientListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(ClientMain.class);

	public static void main(String[] args) {
		new ClientMain();
	}

	public ClientMain() {
		Map<String, String> userHeaders = new HashMap<String, String>();
		userHeaders.put("key1", "value1");

		for (int i = 0; i < 5; i++) {
			final int id = i;
			StompClient client = new StompClient("127.0.0.1", 61626);
			client.addListener(ClientMain.this);
			client.connect();

			client.subscribe("/topic", false);

			if (id % 2 == 0) {
				// try {
				// Thread.sleep(2000);
				// } catch (InterruptedException e) {
				// e.printStackTrace();
				// }
				client.send("/topic", "plop-" + id, userHeaders, false);
			}

			// }
			//
			// try {
			// Thread.sleep(2000);
			// } catch (InterruptedException e) {
			// e.printStackTrace();
			// }
		}
	}

	@Override
	public void connected() {
		LOGGER.debug("Connect to server");
	}

	@Override
	public void disconnected() {
		LOGGER.debug("Disconnect from server");
	}

	@Override
	public void receivedMessage(String topic, String message, Map<String, String> userHeaders) {
		LOGGER.debug("Received data on topic {} - userHeaders = {} : {}", new Object[] { topic, userHeaders, message });
	}

	@Override
	public void receivedError(String shortMessage, String description) {
		LOGGER.error("STOMP Error '{}' : {}", shortMessage, description);
	}
}
