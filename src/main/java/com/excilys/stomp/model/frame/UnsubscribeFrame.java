/**
###############################################################################
# Contributors:
#     Damien VILLENEUVE - initial API and implementation
###############################################################################
 */
package com.excilys.stomp.model.frame;

import com.excilys.stomp.model.Frame;
import com.excilys.stomp.model.Header;

/**
 * @author dvilleneuve
 * 
 */
public class UnsubscribeFrame extends Frame {

	public UnsubscribeFrame(String subscribeId) {
		super(Frame.COMMAND_UNSUBSCRIBE, new Header().set(Header.HEADER_SUBSCRIPTION_ID, String.valueOf(subscribeId)), null);
	}

}
