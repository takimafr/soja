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
public class UnsubscribeFrame extends Frame {

	public UnsubscribeFrame(Long subscribeId) {
		super(Frame.COMMAND_UNSUBSCRIBE, new Header().set(Header.HEADER_SUBSCRIPTION_ID, subscribeId.toString()), null);
	}

}
