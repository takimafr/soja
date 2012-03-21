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
public class AckFrame extends Frame {

	public AckFrame(String messageId, Long subscriptionId) {
		super(Frame.COMMAND_ACK, new Header().set(Header.HEADER_MESSAGE_ID, messageId).set(Header.HEADER_SUBSCRIPTION,
				subscriptionId.toString()), null);
	}

}
