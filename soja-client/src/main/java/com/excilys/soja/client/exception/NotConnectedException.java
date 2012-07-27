/**
###############################################################################
# Contributors:
#     Damien V. - initial API and implementation
###############################################################################
 */
package com.excilys.soja.client.exception;

/**
 * @author dvilleneuve
 * 
 */
public class NotConnectedException extends RuntimeException {

	private static final long serialVersionUID = 2854370887908894728L;

	/**
	 * 
	 */
	public NotConnectedException() {
		super();

	}

	/**
	 * @param message
	 * @param cause
	 */
	public NotConnectedException(String message, Throwable cause) {
		super(message, cause);

	}

	/**
	 * @param message
	 */
	public NotConnectedException(String message) {
		super(message);

	}

	/**
	 * @param cause
	 */
	public NotConnectedException(Throwable cause) {
		super(cause);

	}

}
