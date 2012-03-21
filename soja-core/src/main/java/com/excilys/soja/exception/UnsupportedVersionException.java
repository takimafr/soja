/**
###############################################################################
# Contributors:
#     Damien VILLENEUVE - initial API and implementation
###############################################################################
 */
package com.excilys.soja.exception;

/**
 * @author dvilleneuve
 *
 */
public class UnsupportedVersionException extends RuntimeException {

	private static final long serialVersionUID = 1754782663491146018L;

	/**
	 * 
	 */
	public UnsupportedVersionException() {
		super();

	}

	/**
	 * @param message
	 * @param cause
	 */
	public UnsupportedVersionException(String message, Throwable cause) {
		super(message, cause);

	}

	/**
	 * @param message
	 */
	public UnsupportedVersionException(String message) {
		super(message);

	}

	/**
	 * @param cause
	 */
	public UnsupportedVersionException(Throwable cause) {
		super(cause);

	}

}
