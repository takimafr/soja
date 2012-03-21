/**
###############################################################################
# Contributors:
#     Damien VILLENEUVE - initial API and implementation
###############################################################################
 */
package com.excilys.soja.core.model.frame;

import com.excilys.soja.core.model.Ack;
import com.excilys.soja.core.model.Frame;
import com.excilys.soja.core.model.Header;

/**
 * @author dvilleneuve
 * 
 */
public class SubscribeFrame extends Frame {

	private static long subscribeCount = 0;

	public SubscribeFrame(String topic) {
		super(Frame.COMMAND_SUBSCRIBE, new Header().set(Header.HEADER_DESTINATION, topic).set(
				Header.HEADER_SUBSCRIPTION_ID, String.valueOf(subscribeCount++)), null);
	}

	public void setAck(Ack ack) {
		getHeader().set(Header.HEADER_ACK, ack.toString());
	}

	public long getSubscriptionId() {
		return Long.valueOf(getHeaderValue(Header.HEADER_SUBSCRIPTION_ID));
	}

}
