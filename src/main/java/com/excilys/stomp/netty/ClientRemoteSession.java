/**
###############################################################################
# Contributors:
#     Damien VILLENEUVE - initial API and implementation
###############################################################################
 */
package com.excilys.stomp.netty;

import java.util.Set;

import javax.security.auth.login.LoginException;

import org.apache.commons.lang.ArrayUtils;
import org.jboss.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.excilys.stomp.StompServer;
import com.excilys.stomp.authentication.Authentication;
import com.excilys.stomp.exception.UnsupportedVersionException;
import com.excilys.stomp.model.Frame;
import com.excilys.stomp.model.Header;
import com.excilys.stomp.model.frame.ConnectedFrame;
import com.excilys.stomp.model.frame.ErrorFrame;
import com.excilys.stomp.model.frame.MessageFrame;
import com.excilys.stomp.model.frame.ReceiptFrame;

/**
 * @author dvilleneuve
 * 
 */
public class ClientRemoteSession implements Comparable<ClientRemoteSession> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ClientRemoteSession.class);

	private final Channel channel;
	private Authentication authentication;
	private String sessionToken;

	// subscriptions (topic + ack)
	// token

	public ClientRemoteSession(Channel channel, Authentication authentication) {
		this.channel = channel;
		this.authentication = authentication;
	}

	/**
	 * Send a STOMP frame to the remote
	 * 
	 * @param frame
	 */
	public void sendFrame(Frame frame) {
		LOGGER.trace("Sending : {}", frame);
		channel.write(frame);
	}

	/**
	 * Send a STOMP frame to the remote
	 * 
	 * @param command
	 * @param headers
	 * @param body
	 */
	public void sendFrame(String command, Header headers, String body) {
		sendFrame(new Frame(command, headers, body));
	}

	/**
	 * Send a STOMP error frame. this kind of frame is only available on server-side.
	 * 
	 * @param shortMessage
	 * @param detailedMessage
	 */
	public void sendError(String shortMessage, String detailedMessage) {
		ErrorFrame errorFrame = new ErrorFrame(shortMessage);
		if (detailedMessage != null) {
			errorFrame.setDescription(detailedMessage);
		}
		sendFrame(errorFrame);
	}

	/**
	 * Send a receipt if it was requested
	 * 
	 * @param frame
	 * @return true if a receipt was requests, false else
	 */
	private boolean manageReceipt(Frame frame) {
		// Check if a receipt is asked
		String receipt = frame.getHeaderValue(Header.HEADER_RECEIPT);
		if (receipt != null) {
			sendFrame(new ReceiptFrame(receipt));
			return true;
		}
		return false;
	}

	/**
	 * Handle CONNECT command
	 * 
	 * @param frame
	 */
	public void handleConnect(Frame frame) throws LoginException, UnsupportedVersionException {
		String[] acceptedVersions = frame.getHeaderValue(Header.HEADER_ACCEPT_VERSION).split(",");

		// Check the compatibility of the client and server STOMP version
		if (ArrayUtils.contains(acceptedVersions, StompServer.STOMP_VERSION)) {
			String login = frame.getHeaderValue(Header.HEADER_LOGIN);
			String password = frame.getHeaderValue(Header.HEADER_PASSCODE);

			try {
				// Check the credentials of the user
				synchronized (authentication) {
					sessionToken = authentication.connect(login, password);
				}
				sendFrame(new ConnectedFrame(StompServer.STOMP_VERSION));
			} catch (LoginException e) {
				sendError("Bad credentials", "Username or passcode incorrect");
				throw new LoginException("Login failed for user '" + login + "'");
			}
		} else {
			sendError("Supported version doesn't match", "Supported protocol version is 1.1");
			throw new UnsupportedVersionException();
		}
		return;
	}

	/**
	 * Handle DISCONNECT command
	 * 
	 * @param frame
	 */
	public void handleDisconnect(Frame frame) {
		// Nothing to do here
		manageReceipt(frame);
	}

	/**
	 * Handle SUBSCRIBE command
	 * 
	 * @param frame
	 */
	public void handleSubscribe(Frame frame) {
		String topic = frame.getHeaderValue(Header.HEADER_DESTINATION);

		// TODO: Check for dead-lock
		synchronized (authentication) {
			if (authentication.canSubscribe(sessionToken, topic)) {
				synchronized (SubscriptionManager.class) {
					SubscriptionManager.getInstance().addSubscriber(topic, this);
				}
				manageReceipt(frame);
			} else {
				sendError("Can't subscribe", "You're not allowed to subscribe to the topic" + topic);
			}
		}
	}

	/**
	 * Handle UNSUBSCRIBE command
	 * 
	 * @param frame
	 */
	public void handleUnsubscribe(Frame frame) {
		String topic = frame.getHeaderValue(Header.HEADER_DESTINATION);
		synchronized (SubscriptionManager.class) {
			SubscriptionManager.getInstance().removeSubscriber(topic, this);
		}
		manageReceipt(frame);
	}

	/**
	 * Handle SEND command
	 * 
	 * @param frame
	 */
	public void handleSend(Frame frame) {
		String topic = frame.getHeaderValue(Header.HEADER_DESTINATION);
		String message = frame.getBody();

		// TODO: Check for dead-lock
		synchronized (authentication) {
			if (authentication.canSubscribe(sessionToken, topic)) {
				synchronized (SubscriptionManager.class) {
					Set<ClientRemoteSession> subscribers = SubscriptionManager.getInstance().retrieveSubscribers(topic);
					if (subscribers.size() > 0) {
						Frame messageFrame = new MessageFrame(topic, message, null);

						// Send the message frame to each subscriber
						for (ClientRemoteSession subscriber : subscribers) {
							subscriber.sendFrame(messageFrame);
						}
					}
				}
				manageReceipt(frame);
			} else {
				sendError("Can't send message", "You're not allowed to send a message to the topic" + topic);
			}
		}
	}

	/**
	 * Handle UNKNOWN command
	 * 
	 * @param frame
	 */
	public void handleUnknown(Frame frame) {
		sendError("Unkown command", "The command '" + frame.getCommand() + "' is unkown and can't be managed");
	}

	public Authentication getAuthentication() {
		return authentication;
	}

	public void setAuthentication(Authentication authentication) {
		synchronized (this.authentication) {
			this.authentication = authentication;
		}
	}

	@Override
	public int compareTo(ClientRemoteSession o) {
		return channel.compareTo(o.channel);
	}

}
