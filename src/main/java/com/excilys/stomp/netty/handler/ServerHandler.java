/**
###############################################################################
# Contributors:
#     Damien VILLENEUVE - initial API and implementation
###############################################################################
 */
package com.excilys.stomp.netty.handler;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.security.auth.login.LoginException;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.excilys.stomp.authentication.Authentication;
import com.excilys.stomp.exception.UnsupportedVersionException;
import com.excilys.stomp.model.Ack;
import com.excilys.stomp.model.AckWaiting;
import com.excilys.stomp.model.Frame;
import com.excilys.stomp.model.Header;
import com.excilys.stomp.model.Subscription;
import com.excilys.stomp.model.frame.MessageFrame;
import com.excilys.stomp.netty.ClientRemoteSession;
import com.excilys.stomp.netty.SubscriptionManager;

/**
 * @author dvilleneuve
 * 
 */
public class ServerHandler extends StompHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServerHandler.class);
	private static final String[] SEND_USER_HEADERS_FILTER = new String[] { Header.HEADER_DESTINATION,
			Header.HEADER_TRANSACTION, Header.HEADER_CONTENT_TYPE, Header.HEADER_CONTENT_LENGTH,
			Header.HEADER_RECEIPT_ID_REQUEST };

	private final Authentication authentication;
	private final SubscriptionManager subscriptionManager;

	private Map<Integer, ClientRemoteSession> clientSessions = new HashMap<Integer, ClientRemoteSession>();
	private Map<String, AckWaiting> waitingAcks = new HashMap<String, AckWaiting>();

	public ServerHandler(Authentication authentication) {
		this.authentication = authentication;
		this.subscriptionManager = SubscriptionManager.getInstance();

		LOGGER.debug("craete ServerHandler");
	}

	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		super.channelConnected(ctx, e);
		LOGGER.debug("Client {} connected. Starting client session", ctx.getChannel().getRemoteAddress());

		// When a client connect to the server, we allocate to him a ClientSession instance (based on the channel id)
		Integer channelId = e.getChannel().getId();
		if (!clientSessions.containsKey(channelId)) {
			clientSessions.put(channelId, new ClientRemoteSession(e.getChannel(), authentication));
		}
	}

	@Override
	public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		super.channelDisconnected(ctx, e);
		LOGGER.debug("Client {} disconnected. Stopping client session...", ctx.getChannel().getRemoteAddress());

		disconnectClientSession(ctx.getChannel());
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent event) throws Exception {
		if (!(event.getMessage() instanceof Frame)) {
			LOGGER.error("Not a frame... {}", event.getMessage());
			return;
		}
		Frame frame = (Frame) event.getMessage();
		LOGGER.trace("Received frame : {}", frame);

		// Retrieve the session for this client
		ClientRemoteSession clientRemoteSession = clientSessions.get(event.getChannel().getId());
		if (clientRemoteSession != null) {
			// CONNECT
			if (frame.isCommand(Frame.COMMAND_CONNECT)) {
				try {
					clientRemoteSession.handleConnect(frame);
				} catch (LoginException e) {
					LOGGER.info("Login failed", e);
					disconnectClientSession(event.getChannel());
				} catch (UnsupportedVersionException e) {
					LOGGER.info("The server doesn't support the same STOMP version as the client");
					disconnectClientSession(event.getChannel());
				}
			}
			// DISCONNECT
			else if (frame.isCommand(Frame.COMMAND_DISCONNECT)) {
				clientRemoteSession.handleDisconnect(frame);
				disconnectClientSession(event.getChannel());
			}
			// SUBSCRIBE
			else if (frame.isCommand(Frame.COMMAND_SUBSCRIBE)) {
				clientRemoteSession.handleSubscribe(frame);
			}
			// UNSUBSCRIBE
			else if (frame.isCommand(Frame.COMMAND_UNSUBSCRIBE)) {
				clientRemoteSession.handleUnsubscribe(frame);
			}
			// SEND
			else if (frame.isCommand(Frame.COMMAND_SEND)) {
				handleSend(frame, clientRemoteSession);
			}
			// ACK
			else if (frame.isCommand(Frame.COMMAND_ACK)) {
				handleAck(frame, clientRemoteSession);
			}
			// UNKNOWN
			else {
				clientRemoteSession.handleUnknown(frame);
			}
		}
	}

	/**
	 * Handle SEND command
	 * 
	 * @param sendFrame
	 */
	public void handleSend(Frame sendFrame, ClientRemoteSession clientRemoteSession) {
		String topic = sendFrame.getHeaderValue(Header.HEADER_DESTINATION);

		synchronized (authentication) {
			if (!authentication.canSend(clientRemoteSession.getSessionToken(), topic)) {
				clientRemoteSession.sendError("Can't send message", "You're not allowed to send a message to the topic"
						+ topic);
				return;
			}
		}

		// Retrieve subscribers for the given topic
		Set<Subscription> subscriptions = null;
		synchronized (SubscriptionManager.class) {
			subscriptions = subscriptionManager.retrieveSubscriptions(topic);
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
				subscription.getClientRemoteSession().sendFrame(messageFrame);
			}

			if (acks.size() > 0) {
				synchronized (waitingAcks) {
					String messageId = messageFrame.getMessageId();
					waitingAcks.put(messageId, new AckWaiting(acks, sendFrame, clientRemoteSession));
				}
			} else {
				clientRemoteSession.sendReceiptIfRequested(sendFrame);
			}
		} else {
			clientRemoteSession.sendReceiptIfRequested(sendFrame);
		}
	}

	/**
	 * Handle ACK command
	 * 
	 * @param frame
	 */
	public void handleAck(Frame frame, ClientRemoteSession clientRemoteSession) {
		Long subscriptionId = Long.valueOf(frame.getHeaderValue(Header.HEADER_SUBSCRIPTION));
		String messageId = frame.getHeaderValue(Header.HEADER_MESSAGE_ID);

		synchronized (waitingAcks) {
			// Create a set of subscription which will be used for ACKs requests
			AckWaiting waitingAck = waitingAcks.get(messageId);
			if (waitingAck != null) {
				waitingAck.removeSubscriptionId(subscriptionId);

				LOGGER.debug("ACK processed for message '" + messageId + "' and subscription '" + subscriptionId
						+ "'. Ack left : " + waitingAck.getSubscriptionIds().size());

				if (waitingAck.getSubscriptionIds().size() == 0) {
					waitingAck.getSendClientSession().sendReceiptIfRequested(waitingAck.getSendFrame());
					waitingAcks.remove(messageId);
				}
			}
		}
	}

	private void disconnectClientSession(Channel channel) {
		SocketAddress remoteAddress = channel.getRemoteAddress();
		LOGGER.debug("Disconnecting client session {}", remoteAddress);
		channel.close().awaitUninterruptibly(15000);
		channel.unbind().awaitUninterruptibly(15000);
		clientSessions.remove(channel.getId());
		LOGGER.debug("Client session {} disconnected", remoteAddress);
	}

}
