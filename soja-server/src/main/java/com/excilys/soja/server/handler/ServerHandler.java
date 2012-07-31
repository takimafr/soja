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
package com.excilys.soja.server.handler;

import static com.excilys.soja.core.model.Frame.COMMAND_ACK;
import static com.excilys.soja.core.model.Frame.COMMAND_CONNECT;
import static com.excilys.soja.core.model.Frame.COMMAND_DISCONNECT;
import static com.excilys.soja.core.model.Frame.COMMAND_HEARBEAT;
import static com.excilys.soja.core.model.Frame.COMMAND_SEND;
import static com.excilys.soja.core.model.Frame.COMMAND_SUBSCRIBE;
import static com.excilys.soja.core.model.Frame.COMMAND_UNSUBSCRIBE;
import static com.excilys.soja.core.model.Header.HEADER_ACCEPT_VERSION;
import static com.excilys.soja.core.model.Header.HEADER_ACK;
import static com.excilys.soja.core.model.Header.HEADER_CONTENT_LENGTH;
import static com.excilys.soja.core.model.Header.HEADER_CONTENT_TYPE;
import static com.excilys.soja.core.model.Header.HEADER_DESTINATION;
import static com.excilys.soja.core.model.Header.HEADER_LOGIN;
import static com.excilys.soja.core.model.Header.HEADER_MESSAGE_ID;
import static com.excilys.soja.core.model.Header.HEADER_PASSCODE;
import static com.excilys.soja.core.model.Header.HEADER_RECEIPT_ID_REQUEST;
import static com.excilys.soja.core.model.Header.HEADER_SUBSCRIPTION;
import static com.excilys.soja.core.model.Header.HEADER_SUBSCRIPTION_ID;
import static com.excilys.soja.core.model.Header.HEADER_TRANSACTION;
import static com.excilys.soja.server.StompServer.STOMP_VERSION;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.security.auth.login.LoginException;

import org.apache.commons.lang.ArrayUtils;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.excilys.soja.core.handler.StompHandler;
import com.excilys.soja.core.model.Ack;
import com.excilys.soja.core.model.Frame;
import com.excilys.soja.core.model.frame.ConnectedFrame;
import com.excilys.soja.core.model.frame.ErrorFrame;
import com.excilys.soja.core.model.frame.MessageFrame;
import com.excilys.soja.core.utils.FrameFactory;
import com.excilys.soja.server.authentication.Authentication;
import com.excilys.soja.server.events.StompServerListener;
import com.excilys.soja.server.exception.AlreadyConnectedException;
import com.excilys.soja.server.exception.UnsupportedVersionException;
import com.excilys.soja.server.manager.SubscriptionManager;
import com.excilys.soja.server.model.AckWaiting;
import com.excilys.soja.server.model.Subscription;

/**
 * @author dvilleneuve
 * 
 */
public class ServerHandler extends StompHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServerHandler.class);
	private static final String[] SEND_USER_HEADERS_FILTER = new String[] { HEADER_DESTINATION, HEADER_TRANSACTION,
			HEADER_CONTENT_TYPE, HEADER_CONTENT_LENGTH, HEADER_RECEIPT_ID_REQUEST };
	private static final SubscriptionManager subscriptionManager = SubscriptionManager.getInstance();
	private static final Map<String, AckWaiting> waitingAcks = new HashMap<String, AckWaiting>();

	private final List<StompServerListener> stompServerListeners = new ArrayList<StompServerListener>();
	private final Authentication authentication;
	private final Map<Channel, String> clientsSessionToken = new HashMap<Channel, String>();

	public ServerHandler(Authentication authentication) {
		this.authentication = authentication;
	}

	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		super.channelConnected(ctx, e);
		LOGGER.debug("Channel connected to {}. Starting client session", ctx.getChannel().getRemoteAddress());
	}

	@Override
	public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		super.channelDisconnected(ctx, e);
		handleDisconnectingClient(ctx.getChannel());
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		LOGGER.debug("Exception thrown by Netty. Closing channel : {}", e.getCause());
		ctx.getChannel().close().awaitUninterruptibly(2000);
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent event) throws Exception {
		if (!(event.getMessage() instanceof Frame)) {
			LOGGER.error("Not a frame... {}", event.getMessage());
			return;
		}
		Channel channel = ctx.getChannel();
		Frame frame = (Frame) event.getMessage();
		LOGGER.trace("Received frame : {}", frame);

		// CONNECT
		if (frame.isCommand(COMMAND_CONNECT)) {
			try {
				handleConnect(channel, frame);
			} catch (RuntimeException e) {
				LOGGER.info("Login failed", e);
				disconnectClient(event.getChannel());
			}
		}
		// DISCONNECT
		else if (frame.isCommand(COMMAND_DISCONNECT)) {
			handleDisconnect(channel, frame);
			disconnectClient(event.getChannel());
		}
		// SUBSCRIBE
		else if (frame.isCommand(COMMAND_SUBSCRIBE)) {
			handleSubscribe(channel, frame);
		}
		// UNSUBSCRIBE
		else if (frame.isCommand(COMMAND_UNSUBSCRIBE)) {
			handleUnsubscribe(channel, frame);
		}
		// SEND
		else if (frame.isCommand(COMMAND_SEND)) {
			handleSend(channel, frame);
		}
		// ACK
		else if (frame.isCommand(COMMAND_ACK)) {
			handleAck(channel, frame);
		}
		// HEARTBEAT
		else if (frame.isCommand(COMMAND_HEARBEAT)) {
			handleHeartBeat(channel, frame);
		}
		// UNKNOWN
		else {
			handleUnknown(channel, frame);
		}
	}

	/**
	 * Handle CONNECT command
	 * 
	 * @param frame
	 * @throws SocketException
	 */
	public void handleConnect(final Channel channel, Frame frame) throws LoginException, UnsupportedVersionException,
			AlreadyConnectedException, SocketException {
		// Retrieve the session for this client
		if (clientsSessionToken.containsKey(channel)) {
			throw new AlreadyConnectedException("User try to connect but it seems to be already connected");
		}

		String[] acceptedVersions = frame.getHeader().get(HEADER_ACCEPT_VERSION, "").split(",");

		// Check the compatibility of the client and server STOMP version
		if (!ArrayUtils.contains(acceptedVersions, STOMP_VERSION)) {
			sendError(channel, "Supported version doesn't match", "Supported protocol version is " + STOMP_VERSION);
			throw new UnsupportedVersionException(
					"The server doesn't support the same STOMP version as the client : server=" + STOMP_VERSION
							+ ", client=" + acceptedVersions);
		}

		String login = frame.getHeaderValue(HEADER_LOGIN);
		String password = frame.getHeaderValue(HEADER_PASSCODE);

		try {
			LOGGER.trace("Check credentials for {}", login);

			// Check the credentials of the user
			String clientSessionToken = authentication.connect(login, password);
			clientsSessionToken.put(channel, clientSessionToken);

			// Create the frame to send
			ConnectedFrame connectedFrame = new ConnectedFrame(STOMP_VERSION);

			// Start the heart-beat scheduler if needed
			if (startLocalHeartBeat(channel, frame)) {
				connectedFrame.setHeartBeat(getLocalGuaranteedHeartBeat(), getLocalExpectedHeartBeat());
			}

			sendFrame(channel, connectedFrame);
			
			fireConnectedListeners(channel);
		} catch (LoginException e) {
			sendError(channel, "Bad credentials", "Username or passcode incorrect");
			throw e;
		}
	}

	/**
	 * Handle DISCONNECT command
	 * 
	 * @param frame
	 * @throws SocketException
	 */
	public void handleDisconnect(Channel channel, Frame frame) throws SocketException {
		sendReceiptIfRequested(channel, frame);

		handleDisconnectingClient(channel);
	}

	/**
	 * Handle SEND command
	 * 
	 * @param sendFrame
	 * @throws SocketException
	 */
	public void handleSend(Channel channel, Frame sendFrame) throws SocketException {
		String topic = sendFrame.getHeaderValue(HEADER_DESTINATION);

		synchronized (authentication) {
			if (!authentication.canSend(clientsSessionToken.get(channel), topic)) {
				sendError(channel, "Can't send message", "You're not allowed to send a message to the topic" + topic);
				return;
			}
		}

		// Retrieve subscribers for the given topic
		Set<Subscription> subscriptions = null;
		subscriptions = subscriptionManager.retrieveSubscriptionsByTopic(topic);

		if (subscriptions != null && subscriptions.size() > 0) {
			sendReceiptIfRequested(channel, sendFrame);
			return;
		}

		// Construct the MESSAGE frame
		MessageFrame messageFrame = new MessageFrame(topic, sendFrame.getBody(), null);

		// Add content-type if it was present on the SEND command
		String contentType = sendFrame.getHeaderValue(HEADER_CONTENT_TYPE);
		if (contentType != null) {
			messageFrame.setContentType(contentType);
		}

		// Add user keys if there was some on the SEND command
		Set<String> userKeys = sendFrame.getHeader().allKeys(SEND_USER_HEADERS_FILTER);
		for (String userKey : userKeys) {
			messageFrame.getHeader().put(userKey, sendFrame.getHeaderValue(userKey));
		}

		// Create a set of subscription which will be used for ACKs requests
		TreeSet<Long> acks = new TreeSet<Long>();

		// Send the message frame to each subscriber
		for (Subscription subscription : subscriptions) {
			// If an ack is needed for this client, add the subscription to the ACKs queue.
			if (subscription.getAckMode() != Ack.AUTO) {
				acks.add(subscription.getSubscriptionId());
			}

			messageFrame.setHeaderValue(HEADER_SUBSCRIPTION, subscription.getSubscriptionId().toString());
			sendFrame(subscription.getChannel(), messageFrame);
		}

		if (acks.size() > 0) {
			synchronized (waitingAcks) {
				String messageId = messageFrame.getMessageId();
				waitingAcks.put(messageId, new AckWaiting(channel, acks, sendFrame));
			}
		} else {
			sendReceiptIfRequested(channel, sendFrame);
		}
	}

	/**
	 * Handle SUBSCRIBE command
	 * 
	 * @param frame
	 * @throws SocketException
	 */
	public void handleSubscribe(Channel channel, Frame frame) throws SocketException {
		String topic = frame.getHeaderValue(HEADER_DESTINATION);
		Long subscriptionId = Long.valueOf(frame.getHeaderValue(HEADER_SUBSCRIPTION_ID));
		Ack ackMode = Ack.parseAck(frame.getHeaderValue(HEADER_ACK));

		String clientSessionToken = clientsSessionToken.get(channel);
		if (authentication.canSubscribe(clientSessionToken, topic)) {
			subscriptionManager.addSubscription(channel, clientSessionToken, subscriptionId, topic, ackMode);
			sendReceiptIfRequested(channel, frame);
		} else {
			sendError(channel, "Can't subscribe", "You're not allowed to subscribe to the topic" + topic);
		}
	}

	/**
	 * Handle UNSUBSCRIBE command
	 * 
	 * @param frame
	 * @throws SocketException
	 */
	public void handleUnsubscribe(Channel channel, Frame frame) throws SocketException {
		Long subscriptionId = Long.valueOf(frame.getHeaderValue(HEADER_SUBSCRIPTION_ID));

		subscriptionManager.removeSubscription(clientsSessionToken.get(channel), subscriptionId);
		sendReceiptIfRequested(channel, frame);
	}

	/**
	 * Handle ACK command
	 * 
	 * @param frame
	 * @throws SocketException
	 */
	public void handleAck(Channel channel, Frame frame) throws SocketException {
		Long subscriptionId = Long.valueOf(frame.getHeaderValue(HEADER_SUBSCRIPTION));
		String messageId = frame.getHeaderValue(HEADER_MESSAGE_ID);

		synchronized (waitingAcks) {
			// Create a set of subscription which will be used for ACKs requests
			AckWaiting waitingAck = waitingAcks.get(messageId);
			if (waitingAck != null) {
				waitingAck.removeSubscriptionId(subscriptionId);

				if (waitingAck.getSubscriptionIds().isEmpty()) {
					sendReceiptIfRequested(waitingAck.getChannel(), waitingAck.getSendFrame());
					waitingAcks.remove(messageId);
				}
			}
		}
	}

	/**
	 * Handle UNKNOWN command
	 * 
	 * @param frame
	 * @throws SocketException
	 */
	public void handleUnknown(Channel channel, Frame frame) throws SocketException {
		sendError(channel, "Unkown command", "The command '" + frame.getCommand() + "' is unkown and can't be managed");
	}

	/**
	 * Send a STOMP error frame. this kind of frame is only available on server-side.
	 * 
	 * @param shortMessage
	 * @param detailedMessage
	 * @throws SocketException
	 */
	public void sendError(Channel channel, String shortMessage, String detailedMessage) throws SocketException {
		ErrorFrame errorFrame = new ErrorFrame(shortMessage);
		if (detailedMessage != null) {
			errorFrame.setDescription(detailedMessage);
		}
		sendFrame(channel, errorFrame);
	}

	/**
	 * Send a receipt if it was requested
	 * 
	 * @param frame
	 * @return true if a receipt was requests, false else
	 * @throws SocketException
	 */
	public boolean sendReceiptIfRequested(Channel channel, Frame frame) throws SocketException {
		Frame receiptFrame = FrameFactory.createReceipt(frame);
		if (receiptFrame != null) {
			sendFrame(channel, receiptFrame);
			return true;
		}
		return false;
	}

	public void disconnectAllClients() {
		Set<Channel> channelClientsSet = clientsSessionToken.keySet();
		for (Channel channelClient : channelClientsSet) {
			disconnectClient(channelClient);
		}
	}

	private void disconnectClient(Channel channel) {
		LOGGER.debug("Disconnecting client {}...", channel.getRemoteAddress());

		channel.close().awaitUninterruptibly(15000);
		channel.unbind().awaitUninterruptibly(15000);

		handleDisconnectingClient(channel);
	}

	public void addListener(StompServerListener stompServerListener) {
		stompServerListeners.add(stompServerListener);
	}

	public void removeListener(StompServerListener stompServerListener) {
		stompServerListeners.remove(stompServerListener);
	}

	/**
	 * Clean session information and remove subscriptions for this channel then notify all listeners
	 * 
	 * @param channel
	 */
	private void handleDisconnectingClient(Channel channel) {
		// Remove all subscription for this client's session
		subscriptionManager.removeSubscriptions(clientsSessionToken.get(channel));
		// Remote session token
		clientsSessionToken.remove(channel);
		fireDisconnectedListeners(channel);
	}

	protected void fireConnectedListeners(Channel channel) {
		for (StompServerListener stompServerListener : stompServerListeners) {
			stompServerListener.clientConnected(channel.getRemoteAddress());
		}
	}

	protected void fireDisconnectedListeners(Channel channel) {
		for (StompServerListener stompServerListener : stompServerListeners) {
			stompServerListener.clientDisconnected(channel.getRemoteAddress());
		}
	}

}
