/**
###############################################################################
# Contributors:
#     Damien VILLENEUVE - initial API and implementation
###############################################################################
 */
package com.excilys.stomp.utils;

import com.excilys.stomp.model.Frame;
import com.excilys.stomp.model.Header;
import com.excilys.stomp.model.frame.ReceiptFrame;

/**
 * @author dvilleneuve
 * 
 */
public class FrameFactory {

	/**
	 * Construct and return a RECEIPT frame if a receipt was asked by the client. Else, return null
	 * 
	 * @param frame
	 *            the frame sent by the client
	 * @return a ReceiptFrame instance or null
	 */
	public static Frame createReceipt(Frame frame) {
		// Check if a receipt is asked
		String receipt = frame.getHeaderValue(Header.HEADER_RECEIPT_ID_REQUEST);
		if (receipt != null) {
			return new ReceiptFrame(receipt);
		}
		return null;
	}

}
