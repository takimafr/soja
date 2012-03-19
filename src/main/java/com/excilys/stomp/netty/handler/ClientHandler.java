/**
###############################################################################
# Contributors:
#     Damien VILLENEUVE - initial API and implementation
###############################################################################
 */
package com.excilys.stomp.netty.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.excilys.stomp.events.StompClientListener;
import com.excilys.stomp.events.StompMessageStateCallback;
import com.excilys.stomp.model.Frame;
import com.excilys.stomp.model.Header;

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
	private final Map<String, StompMessageStateCallback> stompMessagesCallbacks = new HashMap<String, StompMessageStateCallback>();

	private static long messageSent = 0;

	private boolean connected = false;

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		if (!(e.getMessage() instanceof Frame)) {
			LOGGER.error("Not a frame... {}", e.getMessage());
			return;
		}
		Frame frame = (Frame) e.getMessage();

		// ERROR
		if (frame.isCommand(Frame.COMMAND_ERROR)) {
			LOGGER.error("STOMP error '{}' : {}", frame.getHeaderValue(Header.HEADER_MESSAGE), frame.getBody());
		} else {
			LOGGER.trace("Received frame : {}", frame);

			// CONNECTED
			if (frame.isCommand(Frame.COMMAND_CONNECTED)) {
				handleConnected(frame);
			}
			// MESSAGE
			else if (frame.isCommand(Frame.COMMAND_MESSAGE)) {
				handleMessage(frame);
			}
			// RECEIPT
			else if (frame.isCommand(Frame.COMMAND_RECEIPT)) {
				handleReceipt(frame);
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
	private void handleConnected(Frame frame) {
		connected = true;

		// Notify all listeners
		for (StompClientListener listener : stompClientListeners) {
			listener.connected();
		}
	}

	/**
	 * Handle MESSAGE command
	 * 
	 * @param frame
	 */
	private void handleMessage(Frame frame) {
		String topic = frame.getHeaderValue(Header.HEADER_DESTINATION);
		String message = frame.getBody();

		// Retrieve user keys
		Map<String, String> userHeaders = new HashMap<String, String>();
		Set<String> userKeys = frame.getHeader().allKeys(MESSAGE_USER_HEADERS_FILTER);
		for (String userKey : userKeys) {
			userHeaders.put(userKey, frame.getHeaderValue(userKey));
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
	private void handleReceipt(Frame frame) {
		String receiptId = frame.getHeaderValue(Header.HEADER_RECEIPT_ID_RESPONSE);
		if (receiptId != null) {
			synchronized (stompMessagesCallbacks) {
				StompMessageStateCallback messageCallback = stompMessagesCallbacks.remove(receiptId);
				if (messageCallback != null) {
					messageCallback.onMessageSent();
				}
			}
		}
	}

	/**
	 * Add a receipt and send the frame. When the server has successfully processed the client's request, he send back a
	 * receipt to notify the client.
	 * 
	 * @param frame
	 * @param callback
	 */
	public void sendFrame(Frame frame, StompMessageStateCallback callback) {
		if (callback != null) {
			synchronized (stompMessagesCallbacks) {
				String receiptId = frame.getCommand() + messageSent++;
				stompMessagesCallbacks.put(receiptId, callback);
				frame.getHeader().put(Header.HEADER_RECEIPT_ID_REQUEST, receiptId);
			}
		}
		sendFrame(frame);
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
