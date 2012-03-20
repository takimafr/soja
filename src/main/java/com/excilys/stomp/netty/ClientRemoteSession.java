/**
###############################################################################
# Contributors:
#     Damien VILLENEUVE - initial API and implementation
###############################################################################
 */
package com.excilys.stomp.netty;

import javax.security.auth.login.LoginException;

import org.apache.commons.lang.ArrayUtils;
import org.jboss.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.excilys.stomp.StompServer;
import com.excilys.stomp.authentication.Authentication;
import com.excilys.stomp.exception.UnsupportedVersionException;
import com.excilys.stomp.model.Ack;
import com.excilys.stomp.model.Frame;
import com.excilys.stomp.model.Header;
import com.excilys.stomp.model.frame.ConnectedFrame;
import com.excilys.stomp.model.frame.ErrorFrame;
import com.excilys.stomp.utils.FrameFactory;

/**
 * @author dvilleneuve
 * 
 */
public class ClientRemoteSession implements Comparable<ClientRemoteSession> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ClientRemoteSession.class);

	private final Channel channel;
	private final Authentication authentication;
	private final SubscriptionManager subscriptionManager;
	private String sessionToken;

	public ClientRemoteSession(Channel channel, Authentication authentication) {
		this.channel = channel;
		this.authentication = authentication;
		this.subscriptionManager = SubscriptionManager.getInstance();
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
	public boolean sendReceiptIfRequested(Frame frame) {
		Frame receiptFrame = FrameFactory.createReceipt(frame);
		if (receiptFrame != null) {
			sendFrame(receiptFrame);
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
		String[] acceptedVersions = frame.getHeader().get(Header.HEADER_ACCEPT_VERSION, "").split(",");

		// Check the compatibility of the client and server STOMP version
		if (ArrayUtils.contains(acceptedVersions, StompServer.STOMP_VERSION)) {
			String login = frame.getHeaderValue(Header.HEADER_LOGIN);
			String password = frame.getHeaderValue(Header.HEADER_PASSCODE);

			try {
				// Check the credentials of the user
				sessionToken = authentication.connect(login, password);
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
		// Remove all subscription for this client's session
		subscriptionManager.removeSubscriptions(this);

		sendReceiptIfRequested(frame);
	}

	/**
	 * Handle SUBSCRIBE command
	 * 
	 * @param frame
	 */
	public void handleSubscribe(Frame frame) {
		String topic = frame.getHeaderValue(Header.HEADER_DESTINATION);
		Long subscriptionId = Long.valueOf(frame.getHeaderValue(Header.HEADER_SUBSCRIPTION_ID));
		Ack ackMode = Ack.parseAck(frame.getHeaderValue(Header.HEADER_ACK));

		if (authentication.canSubscribe(sessionToken, topic)) {
			synchronized (SubscriptionManager.class) {
				subscriptionManager.addSubscription(this, subscriptionId, topic, ackMode);
			}
			sendReceiptIfRequested(frame);
		} else {
			sendError("Can't subscribe", "You're not allowed to subscribe to the topic" + topic);
		}
	}

	/**
	 * Handle UNSUBSCRIBE command
	 * 
	 * @param frame
	 */
	public void handleUnsubscribe(Frame frame) {
		Long subscriptionId = Long.valueOf(frame.getHeaderValue(Header.HEADER_SUBSCRIPTION_ID));

		synchronized (SubscriptionManager.class) {
			subscriptionManager.removeSubscription(this, subscriptionId);
		}
		sendReceiptIfRequested(frame);
	}

	/**
	 * Handle UNKNOWN command
	 * 
	 * @param frame
	 */
	public void handleUnknown(Frame frame) {
		sendError("Unkown command", "The command '" + frame.getCommand() + "' is unkown and can't be managed");
	}

	public String getSessionToken() {
		return sessionToken;
	}

	public void setSessionToken(String sessionToken) {
		this.sessionToken = sessionToken;
	}

	@Override
	public int compareTo(ClientRemoteSession o) {
		return channel.compareTo(o.channel);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((authentication == null) ? 0 : authentication.hashCode());
		result = prime * result + ((channel == null) ? 0 : channel.hashCode());
		result = prime * result + ((sessionToken == null) ? 0 : sessionToken.hashCode());
		result = prime * result + ((subscriptionManager == null) ? 0 : subscriptionManager.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ClientRemoteSession other = (ClientRemoteSession) obj;
		if (authentication == null) {
			if (other.authentication != null)
				return false;
		} else if (!authentication.equals(other.authentication))
			return false;
		if (channel == null) {
			if (other.channel != null)
				return false;
		} else if (!channel.equals(other.channel))
			return false;
		if (sessionToken == null) {
			if (other.sessionToken != null)
				return false;
		} else if (!sessionToken.equals(other.sessionToken))
			return false;
		if (subscriptionManager == null) {
			if (other.subscriptionManager != null)
				return false;
		} else if (!subscriptionManager.equals(other.subscriptionManager))
			return false;
		return true;
	}

}
