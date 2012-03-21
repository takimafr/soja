/**
###############################################################################
# Contributors:
#     Damien VILLENEUVE - initial API and implementation
###############################################################################
 */
package com.excilys.soja.core.model.frame;

import com.excilys.soja.core.model.Frame;
import com.excilys.soja.core.model.Header;

/**
 * @author dvilleneuve
 * 
 */
public class MessageFrame extends Frame {

	private static long messageCount = 0;

	public MessageFrame(String topic, String message, String subscriptionId) {
		super(Frame.COMMAND_MESSAGE, new Header().set(Header.HEADER_DESTINATION, topic)
				.set(Header.HEADER_MESSAGE_ID, "message-" + (messageCount++))
				.set(Header.HEADER_SUBSCRIPTION, subscriptionId), message);
	}

	public void setContentType(String contentType) {
		getHeader().set(Header.HEADER_CONTENT_TYPE, contentType);
		getHeader().set(Header.HEADER_CONTENT_LENGTH, String.valueOf(getBody().length()));
	}

	public String getMessageId() {
		return getHeaderValue(Header.HEADER_MESSAGE_ID);
	}

}
