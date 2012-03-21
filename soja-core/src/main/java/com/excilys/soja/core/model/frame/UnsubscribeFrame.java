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
public class UnsubscribeFrame extends Frame {

	public UnsubscribeFrame(Long subscribeId) {
		super(Frame.COMMAND_UNSUBSCRIBE, new Header().set(Header.HEADER_SUBSCRIPTION_ID, subscribeId.toString()), null);
	}

}
