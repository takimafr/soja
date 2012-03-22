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
package com.excilys.soja.client;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.excilys.soja.client.StompClient;
import com.excilys.soja.client.events.StompClientListener;
import com.excilys.soja.client.events.StompMessageStateCallback;
import com.excilys.soja.core.model.Ack;

/**
 * @author dvilleneuve
 * 
 */
public class ClientMain {

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
			client.addListener(new StompClientListener() {
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
					LOGGER.debug("Received data on topic {} - userHeaders = {} : {}", new Object[] { topic,
							userHeaders, message });
				}

				@Override
				public void receivedError(String shortMessage, String description) {
					LOGGER.error("STOMP Error '{}' : {}", shortMessage, description);
				}
			});
			if (id == 0) {
//				client.setHeartBeat(15000, 5000);
			}
			client.connect();

			client.subscribe("/topic", new StompMessageStateCallback() {
				@Override
				public void receiptReceived() {
					LOGGER.debug("Subscribe success for " + id);
				}
			}, Ack.CLIENT);

			if (id % 2 == 0) {
				client.send("/topic", "plop-" + id, userHeaders, new StompMessageStateCallback() {
					@Override
					public void receiptReceived() {
						LOGGER.debug("Sending message success for " + id);
					}
				});
			} else {
				client.send("/topic", "simple plop-" + id);
			}
		}
	}
}
