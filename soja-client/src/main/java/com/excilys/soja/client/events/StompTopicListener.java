/**
###############################################################################
# Contributors:
#     Damien VILLENEUVE - initial API and implementation
###############################################################################
 */
package com.excilys.soja.client.events;

import java.util.Map;

import com.excilys.soja.client.handler.ClientHandler;

/**
 * This listener is used to notify the client that a message arrived on the topic he subscribed.<br />
 * A {@link StompClientListener} instance should be linked to a single topic.
 * 
 * @see ClientHandler#subscribe(org.jboss.netty.channel.Channel, String, StompMessageStateCallback,
 *      com.excilys.soja.core.model.Ack)
 * @author dvilleneuve
 * 
 */
public interface StompTopicListener {

	/**
	 * This method is called when the client received a message from the server. If the client who sent the message
	 * added extra headers, they'll be passed as argument.
	 * 
	 * @param message
	 * @param userHeaders
	 *            the extra headers added by the sender
	 */
	void receivedMessage(String message, Map<String, String> userHeaders);

}
