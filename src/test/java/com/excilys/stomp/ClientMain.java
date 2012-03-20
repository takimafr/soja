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
import com.excilys.stomp.events.StompMessageStateCallback;
import com.excilys.stomp.model.Ack;

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

			client.subscribe("/topic", new StompMessageStateCallback() {
				@Override
				public void onMessageSent() {
					LOGGER.debug("Subscribe success for " + id);
				}
			}, Ack.CLIENT);

			if (id % 2 == 0) {
				// try {
				// Thread.sleep(2000);
				// } catch (InterruptedException e) {
				// e.printStackTrace();
				// }
				client.send("/topic", "plop-" + id, userHeaders, new StompMessageStateCallback() {
					@Override
					public void onMessageSent() {
						LOGGER.debug("Sending message success for " + id);
					}
				});
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
		LOGGER.debug("Connected to server");
	}

	@Override
	public void disconnected() {
		LOGGER.debug("Disconnected from server");
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
