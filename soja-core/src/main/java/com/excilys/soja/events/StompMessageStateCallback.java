/**
###############################################################################
# Contributors:
#     Damien VILLENEUVE - initial API and implementation
###############################################################################
 */
package com.excilys.soja.events;

/**
 * This class is used to notify the state of a message sent by a STOMP client.
 * <p/>
 * If the callback is used, the client will automaticaly ask the server for a receipt. When the receipt is received, the
 * method {@link #receiptReceived()} is called. Else, the client application is notified of the error (timeout, error command,
 * etc...) by the {@link #onError(String, String)} method.
 * 
 * @author dvilleneuve
 * 
 */
public interface StompMessageStateCallback {

	/**
	 * This method is called when a message sent by the client has been correctly received and processed by the server.
	 */
	void receiptReceived();

}
