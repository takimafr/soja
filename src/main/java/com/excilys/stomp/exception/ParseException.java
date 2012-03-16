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
public class ParseException extends RuntimeException {

	private static final long serialVersionUID = 3732002938497081374L;

	/**
	 * 
	 */
	public ParseException() {
		super();

	}

	/**
	 * @param message
	 * @param cause
	 */
	public ParseException(String message, Throwable cause) {
		super(message, cause);

	}

	/**
	 * @param message
	 */
	public ParseException(String message) {
		super(message);

	}

	/**
	 * @param cause
	 */
	public ParseException(Throwable cause) {
		super(cause);

	}

}
