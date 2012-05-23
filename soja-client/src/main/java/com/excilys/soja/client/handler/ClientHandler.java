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

import static com.excilys.soja.core.model.Frame.COMMAND_CONNECTED;
import static com.excilys.soja.core.model.Frame.COMMAND_HEARBEAT;
import static com.excilys.soja.core.model.Frame.COMMAND_MESSAGE;
import static com.excilys.soja.core.model.Frame.COMMAND_RECEIPT;
import static com.excilys.soja.core.model.Header.HEADER_CONTENT_LENGTH;
import static com.excilys.soja.core.model.Header.HEADER_CONTENT_TYPE;
import static com.excilys.soja.core.model.Header.HEADER_DESTINATION;
import static com.excilys.soja.core.model.Header.HEADER_HEART_BEAT;
import static com.excilys.soja.core.model.Header.HEADER_MESSAGE;
import static com.excilys.soja.core.model.Header.HEADER_MESSAGE_ID;
import static com.excilys.soja.core.model.Header.HEADER_RECEIPT_ID_REQUEST;
import static com.excilys.soja.core.model.Header.HEADER_RECEIPT_ID_RESPONSE;
import static com.excilys.soja.core.model.Header.HEADER_SUBSCRIPTION;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.excilys.soja.client.events.StompClientListener;
import com.excilys.soja.client.events.StompMessageStateCallback;
import com.excilys.soja.client.events.StompTopicListener;
import com.excilys.soja.client.model.Subscription;
import com.excilys.soja.core.handler.StompHandler;
import com.excilys.soja.core.model.Ack;
import com.excilys.soja.core.model.Frame;
import com.excilys.soja.core.model.frame.AckFrame;
import com.excilys.soja.core.model.frame.ConnectFrame;
import com.excilys.soja.core.model.frame.SendFrame;
import com.excilys.soja.core.model.frame.SubscribeFrame;
import com.excilys.soja.core.model.frame.UnsubscribeFrame;

/**
 * @author dvilleneuve
 * 
 */
public class ClientHandler extends StompHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(ClientHandler.class);
	private static final String[] MESSAGE_USER_HEADERS_FILTER = new String[] { HEADER_DESTINATION, HEADER_SUBSCRIPTION,
			HEADER_MESSAGE_ID, HEADER_CONTENT_TYPE, HEADER_CONTENT_LENGTH };

	private final List<StompClientListener> stompClientListeners = new ArrayList<StompClientListener>();
	private final Map<String, StompMessageStateCallback> messageStateCallbacks = new HashMap<String, StompMessageStateCallback>();
	private final Map<Long, Subscription> subscriptions = new HashMap<Long, Subscription>();

	private static long messageSent = 0;

	private boolean loginRequested = false;
	private boolean loggedIn = false;

	@Override
	public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		super.channelDisconnected(ctx, e);
		LOGGER.debug("Client session {} closed", ctx.getChannel().getRemoteAddress());

		// Notify all listeners
		for (StompClientListener listener : stompClientListeners) {
			listener.disconnected();
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		// LOGGER.warn("Exception throwed by Netty", e.getCause());
		ctx.getChannel().close().awaitUninterruptibly(2000);
	}

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
			LOGGER.error("STOMP error '{}' : {}", frame.getHeaderValue(HEADER_MESSAGE), frame.getBody());
		} else {
			LOGGER.trace("Received frame : {}", frame);

			// CONNECTED
			if (frame.isCommand(COMMAND_CONNECTED)) {
				handleConnected(channel, frame);
			}
			// MESSAGE
			else if (frame.isCommand(COMMAND_MESSAGE)) {
				handleMessage(channel, frame);
			}
			// RECEIPT
			else if (frame.isCommand(COMMAND_RECEIPT)) {
				handleReceipt(channel, frame);
			}
			// HEARTBEAT
			else if (frame.isCommand(COMMAND_HEARBEAT)) {
				handleHeartBeat(channel, frame);
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
	private void handleConnected(final Channel channel, Frame frame) {
		// Start the heart-beat scheduler if needed
		startLocalHeartBeat(channel, frame);

		loggedIn = true;

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
	private void handleMessage(final Channel channel, Frame frame) {
		String message = frame.getBody();
		String topic = frame.getHeaderValue(HEADER_DESTINATION);
		String messageId = frame.getHeaderValue(HEADER_MESSAGE_ID);
		Long subscriptionId = Long.valueOf(frame.getHeaderValue(HEADER_SUBSCRIPTION));

		// Retrieve user keys
		Map<String, String> userHeaders = new HashMap<String, String>();
		Set<String> userKeys = frame.getHeader().allKeys(MESSAGE_USER_HEADERS_FILTER);
		for (String userKey : userKeys) {
			userHeaders.put(userKey, frame.getHeaderValue(userKey));
		}

		// Send an ACK to the server if needed
		Subscription subscription = subscriptions.get(subscriptionId);
		if (subscription != null && subscription.getAckMode() != Ack.AUTO) {
			sendFrame(channel, new AckFrame(messageId, subscriptionId));
		}

		// Notify all listeners
		for (Subscription subscription2 : subscriptions.values()) {
			if (subscription2.getTopic().equals(topic)) {
				subscription2.getTopicListener().receivedMessage(message, userHeaders);
			}
		}
	}

	/**
	 * Handle RECEIPT command
	 * 
	 * @param frame
	 */
	private void handleReceipt(final Channel channel, Frame frame) {
		String receiptId = frame.getHeaderValue(HEADER_RECEIPT_ID_RESPONSE);
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
	 * @return true if the frame has been sent, false if the frame couldn't be sent because the user wasn't try to login
	 */
	public boolean sendFrame(final Channel channel, Frame frame, StompMessageStateCallback callback) {
		if (!isLoginRequested()) {
			String shortMessage = "You're not logged in";
			String description = "You must connect to the server before trying to send a " + frame.getCommand()
					+ " command";

			for (StompClientListener stompClientListener : stompClientListeners) {
				stompClientListener.receivedError(shortMessage, description);
			}
			return false;
		}

		if (callback != null) {
			synchronized (messageStateCallbacks) {
				String receiptId = frame.getCommand() + "-" + messageSent++;
				messageStateCallbacks.put(receiptId, callback);
				frame.getHeader().put(HEADER_RECEIPT_ID_REQUEST, receiptId);
			}
		}
		sendFrame(channel, frame);
		return true;
	}

	/**
	 * Send a CONNECT command frame with heart-beat header if one or both guaranteedHeartBeat and expectedHearBeat are
	 * superior or equals to 0
	 * 
	 * @param channel
	 * @param stompVersionSupported
	 *            STOMP version supported by the client implementation
	 * @param hostname
	 *            a virtual hostname. Can be set to the physical hostname
	 * @param username
	 *            a username to use for connection. Leave <code>null</code> to connect as a guest
	 * @param password
	 *            a password to use for connection. Leave <code>null</code> to connect as a guest
	 */
	public void connect(final Channel channel, String stompVersionSupported, String hostname, String username,
			String password) {
		loginRequested = true;

		ConnectFrame connectFrame = new ConnectFrame(stompVersionSupported, hostname, username, password);
		if (getLocalGuaranteedHeartBeat() > 0 || getLocalExpectedHeartBeat() > 0) {
			LOGGER.debug("Heart-beating activated : {}, {}", getLocalGuaranteedHeartBeat(), getLocalExpectedHeartBeat());
			connectFrame.setHeaderValue(HEADER_HEART_BEAT, getLocalGuaranteedHeartBeat() + ","
					+ getLocalExpectedHeartBeat());
		}
		sendFrame(channel, connectFrame);
	}

	/**
	 * Send a SUBSCRIBE command frame and keep in memory the subscription and the ACK mode for this.
	 * 
	 * @param topic
	 * @param callback
	 * @param ackMode
	 * @return the subscription id
	 */
	public Long subscribe(final Channel channel, String topic, StompTopicListener topicListener,
			StompMessageStateCallback callback, Ack ackMode) {
		SubscribeFrame frame = new SubscribeFrame(topic);
		frame.setAck(ackMode);

		if (!sendFrame(channel, frame, callback)) {
			return -1L;
		}

		Long subscriptionId = frame.getSubscriptionId();

		Subscription subscription = new Subscription(subscriptionId, ackMode, topic, topicListener);
		subscriptions.put(subscriptionId, subscription);
		return subscriptionId;
	}

	/**
	 * Send an UNSUBSCRIBE command frame and remove the subscription and ACK mode for this one from the memory.
	 * 
	 * @param subscriptionId
	 * @param callback
	 */
	public void unsubscribe(final Channel channel, Long subscriptionId, StompMessageStateCallback callback) {
		subscriptions.remove(subscriptionId);

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
	public void send(final Channel channel, String topic, String message, Map<String, String> additionalHeaders,
			StompMessageStateCallback callback) {
		Frame frame = new SendFrame(topic, message);
		if (additionalHeaders != null) {
			frame.getHeader().putAll(additionalHeaders);
		}
		sendFrame(channel, frame, callback);
	}

	/**
	 * @return true if the user try to login (ie: Sent a CONNECT frame). Else, return false
	 */
	public boolean isLoginRequested() {
		return loginRequested;
	}

	/**
	 * @return true if the user is logged in (ie: Sent a CONNECT frame and receive a CONNECTED frame). Else, return
	 *         false
	 */
	public boolean isLoggedIn() {
		return loggedIn;
	}

	public void addListener(StompClientListener stompClientListener) {
		stompClientListeners.add(stompClientListener);
	}

	public void removeListener(StompClientListener stompClientListener) {
		stompClientListeners.remove(stompClientListener);
	}

}
