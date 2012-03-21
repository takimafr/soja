/**
###############################################################################
# Contributors:
#     Damien VILLENEUVE - initial API and implementation
###############################################################################
 */
package com.excilys.soja.utils;

import static com.excilys.soja.model.Frame.COMMAND_ABORT;
import static com.excilys.soja.model.Frame.COMMAND_ACK;
import static com.excilys.soja.model.Frame.COMMAND_BEGIN;
import static com.excilys.soja.model.Frame.COMMAND_COMMIT;
import static com.excilys.soja.model.Frame.COMMAND_CONNECT;
import static com.excilys.soja.model.Frame.COMMAND_CONNECTED;
import static com.excilys.soja.model.Frame.COMMAND_DISCONNECT;
import static com.excilys.soja.model.Frame.COMMAND_ERROR;
import static com.excilys.soja.model.Frame.COMMAND_MESSAGE;
import static com.excilys.soja.model.Frame.COMMAND_NACK;
import static com.excilys.soja.model.Frame.COMMAND_RECEIPT;
import static com.excilys.soja.model.Frame.COMMAND_SEND;
import static com.excilys.soja.model.Frame.COMMAND_SUBSCRIBE;
import static com.excilys.soja.model.Frame.COMMAND_UNSUBSCRIBE;

import java.io.IOException;
import java.util.Scanner;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import com.excilys.soja.exception.ParseException;
import com.excilys.soja.model.Frame;
import com.excilys.soja.model.Header;

/**
 * @author dvilleneuve
 * 
 */
public class FrameParser {

	private static final String[] VALID_COMMANDS = { COMMAND_CONNECT, COMMAND_DISCONNECT, COMMAND_SEND,
			COMMAND_MESSAGE, COMMAND_SUBSCRIBE, COMMAND_UNSUBSCRIBE, COMMAND_BEGIN, COMMAND_COMMIT, COMMAND_ABORT,
			COMMAND_RECEIPT, COMMAND_CONNECTED, COMMAND_ERROR, COMMAND_ACK, COMMAND_NACK };
	private static final String[] COMMANDS_WITH_BODY = { COMMAND_SEND, COMMAND_MESSAGE, COMMAND_ERROR };

	public static Frame parseStream(String frameString) throws IOException, ParseException {
		Scanner scanner = new Scanner(frameString);
		if (!scanner.hasNextLine()) {
			return null;
		}

		// COMMAND
		String command = scanner.nextLine();
		if (!isValidCommand(command)) {
			throw new ParseException(command + " is not a valid command");
		}

		// HEADER
		Header header = new Header();
		String headerLine = null;
		while (scanner.hasNextLine()) {
			headerLine = scanner.nextLine();
			if (headerLine.length() == 0) {
				break;
			}

			String[] headerParts = headerLine.split(":");
			if (headerParts.length == 2) {
				header.put(headerParts[0], unescapeHeaderValue(headerParts[1]));
			}
		}

		// BODY
		String bodyString = null;
		if (isExpectedBody(command)) {
			StringBuilder body = new StringBuilder();
			while (scanner.hasNextLine()) {
				body.append(scanner.nextLine()).append('\n');
			}
			bodyString = StringUtils.chop(body.toString());
		}

		scanner.close();

		return new Frame(command, header, bodyString);
	}

	public static boolean isValidCommand(String command) {
		return command != null && ArrayUtils.contains(VALID_COMMANDS, command);
	}

	public static boolean isExpectedBody(String command) {
		return command != null && ArrayUtils.contains(COMMANDS_WITH_BODY, command);
	}

	public static String unescapeHeaderValue(String headerValue) {
		headerValue = headerValue.replace("\\n", "\n");
		headerValue = headerValue.replace("\\c", ":");
		headerValue = headerValue.replace("\\\\", "\\");

		return headerValue;
	}

}
