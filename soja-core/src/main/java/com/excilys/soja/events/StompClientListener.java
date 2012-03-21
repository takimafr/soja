/**
###############################################################################
# Contributors:
#     Damien VILLENEUVE - initial API and implementation
###############################################################################
 */
package com.excilys.soja.events;

import java.util.Map;

/**
 * @author dvilleneuve
 * 
 */
public interface StompClientListener {

	void connected();

	void disconnected();

	void receivedMessage(String topic, String message, Map<String, String> userHeaders);

	void receivedError(String shortMessage, String description);

}
