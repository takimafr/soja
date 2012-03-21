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
public class DisconnectFrame extends Frame {

	public DisconnectFrame() {
		super(Frame.COMMAND_DISCONNECT, new Header(), null);
	}

}
