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
public class ReceiptFrame extends Frame {

	public ReceiptFrame(String receipId) {
		super(Frame.COMMAND_RECEIPT, new Header().set(Header.HEADER_RECEIPT_ID, receipId), null);
	}

}
