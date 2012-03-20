/**
###############################################################################
# Contributors:
#     Damien VILLENEUVE - initial API and implementation
###############################################################################
 */
package com.excilys.stomp.exception;

/**
 * @author dvilleneuve
 * 
 */
public class AlreadyConnectedException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5368503276483615740L;

	/**
	 * 
	 */
	public AlreadyConnectedException() {
		super();

	}

	/**
	 * @param message
	 * @param cause
	 */
	public AlreadyConnectedException(String message, Throwable cause) {
		super(message, cause);

	}

	/**
	 * @param message
	 */
	public AlreadyConnectedException(String message) {
		super(message);

	}

	/**
	 * @param cause
	 */
	public AlreadyConnectedException(Throwable cause) {
		super(cause);

	}
	
}
