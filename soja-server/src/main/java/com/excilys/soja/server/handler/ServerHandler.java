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

import java.net.SocketAddress;
import java.util.HashMap;
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
import com.excilys.soja.core.model.Header;
import com.excilys.soja.core.model.frame.ConnectedFrame;
import com.excilys.soja.core.model.frame.ErrorFrame;
import com.excilys.soja.core.model.frame.MessageFrame;
import com.excilys.soja.core.utils.FrameFactory;
import com.excilys.soja.server.StompServer;
import com.excilys.soja.server.authentication.Authentication;
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
	private static final String[] SEND_USER_HEADERS_FILTER = new String[] { Header.HEADER_DESTINATION,
			Header.HEADER_TRANSACTION, Header.HEADER_CONTENT_TYPE, Header.HEADER_CONTENT_LENGTH,
			Header.HEADER_RECEIPT_ID_REQUEST };
	private static final SubscriptionManager subscriptionManager = SubscriptionManager.getInstance();
	private static final Map<String, AckWaiting> waitingAcks = new HashMap<String, AckWaiting>();

	private final Authentication authentication;
	private String clientSessionToken;

	public ServerHandler(Authentication authentication) {
		this.authentication = authentication;

		LOGGER.debug("create ServerHandler");
	}

	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		super.channelConnected(ctx, e);
		LOGGER.debug("Client {} connected. Starting client session", ctx.getChannel().getRemoteAddress());
	}

	@Override
	public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		super.channelDisconnected(ctx, e);
		clientSessionToken = null;
		LOGGER.debug("Client session {} closed", ctx.getChannel().getRemoteAddress());
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		super.exceptionCaught(ctx, e);
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
		if (frame.isCommand(Frame.COMMAND_CONNECT)) {
			try {
				handleConnect(channel, frame);
			} catch (RuntimeException e) {
				LOGGER.info("Login failed", e);
				disconnectClient(event.getChannel());
			}
		}
		// DISCONNECT
		else if (frame.isCommand(Frame.COMMAND_DISCONNECT)) {
			handleDisconnect(channel, frame);
			disconnectClient(event.getChannel());
		}
		// SUBSCRIBE
		else if (frame.isCommand(Frame.COMMAND_SUBSCRIBE)) {
			handleSubscribe(channel, frame);
		}
		// UNSUBSCRIBE
		else if (frame.isCommand(Frame.COMMAND_UNSUBSCRIBE)) {
			handleUnsubscribe(channel, frame);
		}
		// SEND
		else if (frame.isCommand(Frame.COMMAND_SEND)) {
			handleSend(channel, frame);
		}
		// ACK
		else if (frame.isCommand(Frame.COMMAND_ACK)) {
			handleAck(channel, frame);
		}
		// HEARTBEAT
		else if (frame.isCommand(Frame.COMMAND_HEARBEAT)) {
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
	 */
	public void handleConnect(final Channel channel, Frame frame) throws LoginException, UnsupportedVersionException,
			AlreadyConnectedException {
		// Retrieve the session for this client
		if (clientSessionToken != null) {
			throw new AlreadyConnectedException("User try to connect but it seems to be already connected");
		}

		String[] acceptedVersions = frame.getHeader().get(Header.HEADER_ACCEPT_VERSION, "").split(",");

		// Check the compatibility of the client and server STOMP version
		if (ArrayUtils.contains(acceptedVersions, StompServer.STOMP_VERSION)) {
			String login = frame.getHeaderValue(Header.HEADER_LOGIN);
			String password = frame.getHeaderValue(Header.HEADER_PASSCODE);

			try {
				// Check the credentials of the user
				clientSessionToken = authentication.connect(login, password);

				// Create the frame to send
				ConnectedFrame connectedFrame = new ConnectedFrame(StompServer.STOMP_VERSION);

				// Start the heart-beat scheduler if needed
				if (startLocalHeartBeat(channel, frame)) {
					connectedFrame.setHeaderValue(Header.HEADER_HEART_BEAT, getLocalGuaranteedHeartBeat() + ","
							+ getLocalExpectedHeartBeat());
				}

				sendFrame(channel, connectedFrame);
			} catch (LoginException e) {
				sendError(channel, "Bad credentials", "Username or passcode incorrect");
				throw new LoginException("Login failed for user '" + login + "'");
			}
		} else {
			sendError(channel, "Supported version doesn't match", "Supported protocol version is "
					+ StompServer.STOMP_VERSION);
			throw new UnsupportedVersionException(
					"The server doesn't support the same STOMP version as the client : server="
							+ StompServer.STOMP_VERSION + ", client=" + acceptedVersions);
		}
	}

	/**
	 * Handle DISCONNECT command
	 * 
	 * @param frame
	 */
	public void handleDisconnect(Channel channel, Frame frame) {
		// Remove all subscription for this client's session
		subscriptionManager.removeSubscriptions(clientSessionToken);

		sendReceiptIfRequested(channel, frame);
	}

	/**
	 * Handle SEND command
	 * 
	 * @param sendFrame
	 */
	public void handleSend(Channel channel, Frame sendFrame) {
		String topic = sendFrame.getHeaderValue(Header.HEADER_DESTINATION);

		synchronized (authentication) {
			if (!authentication.canSend(clientSessionToken, topic)) {
				sendError(channel, "Can't send message", "You're not allowed to send a message to the topic" + topic);
				return;
			}
		}

		// Retrieve subscribers for the given topic
		Set<Subscription> subscriptions = null;
		synchronized (SubscriptionManager.class) {
			subscriptions = subscriptionManager.retrieveSubscriptionsByTopic(topic);
		}

		if (subscriptions != null && subscriptions.size() > 0) {
			// Construct the MESSAGE frame
			MessageFrame messageFrame = new MessageFrame(topic, sendFrame.getBody(), null);

			// Add content-type if it was present on the SEND command
			String contentType = sendFrame.getHeaderValue(Header.HEADER_CONTENT_TYPE);
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

				messageFrame.setHeaderValue(Header.HEADER_SUBSCRIPTION, subscription.getSubscriptionId().toString());
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
		} else {
			sendReceiptIfRequested(channel, sendFrame);
		}
	}

	/**
	 * Handle SUBSCRIBE command
	 * 
	 * @param frame
	 */
	public void handleSubscribe(Channel channel, Frame frame) {
		String topic = frame.getHeaderValue(Header.HEADER_DESTINATION);
		Long subscriptionId = Long.valueOf(frame.getHeaderValue(Header.HEADER_SUBSCRIPTION_ID));
		Ack ackMode = Ack.parseAck(frame.getHeaderValue(Header.HEADER_ACK));

		if (authentication.canSubscribe(clientSessionToken, topic)) {
			synchronized (SubscriptionManager.class) {
				subscriptionManager.addSubscription(channel, clientSessionToken, subscriptionId, topic, ackMode);
			}
			sendReceiptIfRequested(channel, frame);
		} else {
			sendError(channel, "Can't subscribe", "You're not allowed to subscribe to the topic" + topic);
		}
	}

	/**
	 * Handle UNSUBSCRIBE command
	 * 
	 * @param frame
	 */
	public void handleUnsubscribe(Channel channel, Frame frame) {
		Long subscriptionId = Long.valueOf(frame.getHeaderValue(Header.HEADER_SUBSCRIPTION_ID));

		synchronized (SubscriptionManager.class) {
			subscriptionManager.removeSubscription(clientSessionToken, subscriptionId);
		}
		sendReceiptIfRequested(channel, frame);
	}

	/**
	 * Handle ACK command
	 * 
	 * @param frame
	 */
	public void handleAck(Channel channel, Frame frame) {
		Long subscriptionId = Long.valueOf(frame.getHeaderValue(Header.HEADER_SUBSCRIPTION));
		String messageId = frame.getHeaderValue(Header.HEADER_MESSAGE_ID);

		synchronized (waitingAcks) {
			// Create a set of subscription which will be used for ACKs requests
			AckWaiting waitingAck = waitingAcks.get(messageId);
			if (waitingAck != null) {
				waitingAck.removeSubscriptionId(subscriptionId);

				if (waitingAck.getSubscriptionIds().size() == 0) {
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
	 */
	public void handleUnknown(Channel channel, Frame frame) {
		sendError(channel, "Unkown command", "The command '" + frame.getCommand() + "' is unkown and can't be managed");
	}

	/**
	 * Send a STOMP error frame. this kind of frame is only available on server-side.
	 * 
	 * @param shortMessage
	 * @param detailedMessage
	 */
	public void sendError(Channel channel, String shortMessage, String detailedMessage) {
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
	 */
	public boolean sendReceiptIfRequested(Channel channel, Frame frame) {
		Frame receiptFrame = FrameFactory.createReceipt(frame);
		if (receiptFrame != null) {
			sendFrame(channel, receiptFrame);
			return true;
		}
		return false;
	}

	private void disconnectClient(Channel channel) {
		SocketAddress remoteAddress = channel.getRemoteAddress();
		LOGGER.debug("Disconnecting client session {}...", remoteAddress);

		channel.close().awaitUninterruptibly(15000);
		channel.unbind().awaitUninterruptibly(15000);
	}

}
