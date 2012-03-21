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
public class DisconnectFrame extends Frame {

	public DisconnectFrame() {
		super(Frame.COMMAND_DISCONNECT, new Header(), null);
	}

}
