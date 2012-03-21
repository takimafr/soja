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
public class ReceiptFrame extends Frame {

	public ReceiptFrame(String receipId) {
		super(Frame.COMMAND_RECEIPT, new Header().set(Header.HEADER_RECEIPT_ID_RESPONSE, receipId), null);
	}

}
