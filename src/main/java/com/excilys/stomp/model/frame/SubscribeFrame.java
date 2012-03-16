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
public class SubscribeFrame extends Frame {

	private static long subscribeCount = 0;

	public SubscribeFrame(String topic) {
		super(Frame.COMMAND_SUBSCRIBE, new Header().set(Header.HEADER_DESTINATION, topic).set(Header.HEADER_SUBSCRIPTION_ID,
				String.valueOf(subscribeCount++)), null);
	}

	public void setAck(String ack) {
		getHeader().set(Header.HEADER_ACK, ack);
	}
	
}
