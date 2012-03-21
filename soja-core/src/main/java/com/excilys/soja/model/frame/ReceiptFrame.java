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
public class ReceiptFrame extends Frame {

	public ReceiptFrame(String receipId) {
		super(Frame.COMMAND_RECEIPT, new Header().set(Header.HEADER_RECEIPT_ID_RESPONSE, receipId), null);
	}

}
