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
public class DisconnectFrame extends Frame {

	public DisconnectFrame() {
		super(Frame.COMMAND_DISCONNECT, new Header(), null);
	}

}
