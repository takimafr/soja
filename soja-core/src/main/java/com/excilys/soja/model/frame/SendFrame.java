/**
###############################################################################
# Contributors:
#     Damien VILLENEUVE - initial API and implementation
###############################################################################
 */
package com.excilys.soja.model.frame;

import com.excilys.soja.model.Frame;
import com.excilys.soja.model.Header;

/**
 * @author dvilleneuve
 * 
 */
public class SendFrame extends Frame {

	public SendFrame(String topic, String message) {
		super(Frame.COMMAND_SEND, new Header().set(Header.HEADER_DESTINATION, topic), message);
	}

	public void setContentType(String contentType) {
		getHeader().set(Header.HEADER_CONTENT_TYPE, contentType);
		getHeader().set(Header.HEADER_CONTENT_LENGTH, String.valueOf(getBody().length()));
	}

	public void setTransactionId(String transactionId) {
		getHeader().set(Header.HEADER_TRANSACTION, transactionId);
	}

}
