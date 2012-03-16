/**
###############################################################################
# Contributors:
#     Damien VILLENEUVE - initial API and implementation
###############################################################################
 */
package com.excilys.stomp.events;

/**
 * @author dvilleneuve
 * 
 */
public interface StompClientListener {

	void connected();

	void disconnected();

	void receivedMessage(String topic, String message);

	void receivedError(String shortMessage, String description);

}
