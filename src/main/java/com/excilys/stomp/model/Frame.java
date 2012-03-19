/**
###############################################################################
# Contributors:
#     Damien VILLENEUVE - initial API and implementation
###############################################################################
 */
package com.excilys.stomp.model;

/**
 * @author dvilleneuve
 * 
 */
public class Frame {
	public static final String COMMAND_CONNECT = "CONNECT";
	public static final String COMMAND_DISCONNECT = "DISCONNECT";
	public static final String COMMAND_SEND = "SEND";
	public static final String COMMAND_MESSAGE = "MESSAGE";
	public static final String COMMAND_SUBSCRIBE = "SUBSCRIBE";
	public static final String COMMAND_UNSUBSCRIBE = "UNSUBSCRIBE";
	public static final String COMMAND_BEGIN = "BEGIN";
	public static final String COMMAND_COMMIT = "COMMIT";
	public static final String COMMAND_ABORT = "ABORT";

	public static final String COMMAND_RECEIPT = "RECEIPT";
	public static final String COMMAND_CONNECTED = "CONNECTED";
	public static final String COMMAND_ERROR = "ERROR";

	public static final char EOL_COMMAND = '\n';
	public static final char EOL_HEADER = '\n';
	public static final char EOL_HEADERS = '\n';
	public static final char SEPARATOR_HEADER = ':';
	public static final char EOL_FRAME = '\000';

	private String command;
	private Header header;
	private String body;

	public Frame() {
	}

	public Frame(String command, Header header, String body) {
		this.command = command;
		this.header = header;
		this.body = body;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public Header getHeader() {
		return header;
	}

	public void setHeader(Header header) {
		this.header = header;
	}

	/**
	 * Return the value of the requested header. If the header doesn't exists, return null.
	 * 
	 * @param key
	 *            of the header
	 * @return the requested value or null if doesn't exists.
	 */
	public String getHeaderValue(String key) {
		return header.get(key, null);
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public boolean isCommand(String expectedCommand) {
		return command.equalsIgnoreCase(expectedCommand);
	}

	@Override
	public String toString() {
		String formatedBody = (body != null && body.length() > 1000) ? body.substring(0, 1000) + "..." : body;
		return "Frame [command=" + command + ", header=" + header + ", body=" + formatedBody + "]";
	}
}
