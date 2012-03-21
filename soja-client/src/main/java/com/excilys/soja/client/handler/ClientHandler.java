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
package com.excilys.soja.client.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.excilys.soja.client.events.StompClientListener;
import com.excilys.soja.client.events.StompMessageStateCallback;
import com.excilys.soja.core.handler.StompHandler;
import com.excilys.soja.core.model.Ack;
import com.excilys.soja.core.model.Frame;
import com.excilys.soja.core.model.Header;
import com.excilys.soja.core.model.frame.AckFrame;
import com.excilys.soja.core.model.frame.SendFrame;
import com.excilys.soja.core.model.frame.SubscribeFrame;
import com.excilys.soja.core.model.frame.UnsubscribeFrame;

/**
 * @author dvilleneuve
 * 
 */
public class ClientHandler extends StompHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(ClientHandler.class);
	private static final String[] MESSAGE_USER_HEADERS_FILTER = new String[] { Header.HEADER_DESTINATION,
			Header.HEADER_SUBSCRIPTION, Header.HEADER_MESSAGE_ID, Header.HEADER_CONTENT_TYPE,
			Header.HEADER_CONTENT_LENGTH };

	private final List<StompClientListener> stompClientListeners = new ArrayList<StompClientListener>();
	private final Map<String, StompMessageStateCallback> messageStateCallbacks = new HashMap<String, StompMessageStateCallback>();
	private final Map<Long, Ack> subscriptionsAckMode = new HashMap<Long, Ack>();

	private static long messageSent = 0;

	private boolean connected = false;

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		if (!(e.getMessage() instanceof Frame)) {
			LOGGER.error("Not a frame... {}", e.getMessage());
			return;
		}
		Channel channel = ctx.getChannel();
		Frame frame = (Frame) e.getMessage();

		// ERROR
		if (frame.isCommand(Frame.COMMAND_ERROR)) {
			LOGGER.error("STOMP error '{}' : {}", frame.getHeaderValue(Header.HEADER_MESSAGE), frame.getBody());
		} else {
			LOGGER.trace("Received frame : {}", frame);

			// CONNECTED
			if (frame.isCommand(Frame.COMMAND_CONNECTED)) {
				handleConnected(channel, frame);
			}
			// MESSAGE
			else if (frame.isCommand(Frame.COMMAND_MESSAGE)) {
				handleMessage(channel, frame);
			}
			// RECEIPT
			else if (frame.isCommand(Frame.COMMAND_RECEIPT)) {
				handleReceipt(channel, frame);
			}
			// UNKNOWN
			else {
				LOGGER.error("The command '{}' is unkown and can't be managed", frame.getCommand());
			}
		}
	}

	/**
	 * Handle CONNECTED command
	 * 
	 * @param frame
	 */
	private void handleConnected(Channel channel, Frame frame) {
		connected = true;

		// Notify all listeners
		for (StompClientListener listener : stompClientListeners) {
			listener.connected();
		}
	}

	/**
	 * Handle MESSAGE command : send back an ACK to the server if asked during subscription, then notify listeners.
	 * 
	 * @param frame
	 */
	private void handleMessage(Channel channel, Frame frame) {
		String topic = frame.getHeaderValue(Header.HEADER_DESTINATION);
		String message = frame.getBody();
		String messageId = frame.getHeaderValue(Header.HEADER_MESSAGE_ID);
		Long subscriptionId = Long.valueOf(frame.getHeaderValue(Header.HEADER_SUBSCRIPTION));

		// Retrieve user keys
		Map<String, String> userHeaders = new HashMap<String, String>();
		Set<String> userKeys = frame.getHeader().allKeys(MESSAGE_USER_HEADERS_FILTER);
		for (String userKey : userKeys) {
			userHeaders.put(userKey, frame.getHeaderValue(userKey));
		}

		// Send an ACK to the server if needed
		Ack ackMode = subscriptionsAckMode.get(subscriptionId);
		if (ackMode != null && ackMode != Ack.AUTO) {
			sendFrame(channel, new AckFrame(messageId, subscriptionId));
		}

		// Notify all listeners
		for (StompClientListener listener : stompClientListeners) {
			listener.receivedMessage(topic, message, userHeaders);
		}
	}

	/**
	 * Handle RECEIPT command
	 * 
	 * @param frame
	 */
	private void handleReceipt(Channel channel, Frame frame) {
		String receiptId = frame.getHeaderValue(Header.HEADER_RECEIPT_ID_RESPONSE);
		if (receiptId != null) {
			synchronized (messageStateCallbacks) {
				StompMessageStateCallback messageCallback = messageStateCallbacks.remove(receiptId);
				if (messageCallback != null) {
					messageCallback.receiptReceived();
				}
			}
		}
	}

	/**
	 * Add a receipt if a callback is passed and send the frame. When the server has successfully processed the client's
	 * request, he send back a receipt to notify the client.
	 * 
	 * @param frame
	 * @param callback
	 */
	public void sendFrame(Channel channel, Frame frame, StompMessageStateCallback callback) {
		if (callback != null) {
			synchronized (messageStateCallbacks) {
				String receiptId = frame.getCommand() + "-" + messageSent++;
				messageStateCallbacks.put(receiptId, callback);
				frame.getHeader().put(Header.HEADER_RECEIPT_ID_REQUEST, receiptId);
			}
		}
		sendFrame(channel, frame);
	}

	/**
	 * Send a SUBSCRIBE command frame and keep in memory the subscription and the ACK mode for this.
	 * 
	 * @param topic
	 * @param callback
	 * @param ackMode
	 * @return the subscription id
	 */
	public Long subscribe(Channel channel, String topic, StompMessageStateCallback callback, Ack ackMode) {
		SubscribeFrame frame = new SubscribeFrame(topic);
		frame.setAck(ackMode);
		sendFrame(channel, frame, callback);

		Long subscriptionId = frame.getSubscriptionId();
		subscriptionsAckMode.put(subscriptionId, ackMode);
		return subscriptionId;
	}

	/**
	 * Send an UNSUBSCRIBE command frame and remove the subscription and ACK mode for this one from the memory.
	 * 
	 * @param subscriptionId
	 * @param callback
	 */
	public void unsubscribe(Channel channel, Long subscriptionId, StompMessageStateCallback callback) {
		subscriptionsAckMode.remove(subscriptionId);

		Frame frame = new UnsubscribeFrame(subscriptionId);
		sendFrame(channel, frame, callback);
	}

	/**
	 * Send a SEND command frame.
	 * 
	 * @param topic
	 * @param message
	 * @param additionalHeaders
	 * @param callback
	 */
	public void send(Channel channel, String topic, String message, Map<String, String> additionalHeaders,
			StompMessageStateCallback callback) {
		Frame frame = new SendFrame(topic, message);
		if (additionalHeaders != null) {
			frame.getHeader().putAll(additionalHeaders);
		}
		sendFrame(channel, frame, callback);
	}

	public boolean isConnected() {
		return connected;
	}

	public void addListener(StompClientListener stompClientListener) {
		stompClientListeners.add(stompClientListener);
	}

	public void removeListener(StompClientListener stompClientListener) {
		stompClientListeners.remove(stompClientListener);
	}

}
