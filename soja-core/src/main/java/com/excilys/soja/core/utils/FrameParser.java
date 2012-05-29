/**
 * Copyright 2010-2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.excilys.soja.core.utils;

import static com.excilys.soja.core.model.Frame.COMMAND_ABORT;
import static com.excilys.soja.core.model.Frame.COMMAND_ACK;
import static com.excilys.soja.core.model.Frame.COMMAND_BEGIN;
import static com.excilys.soja.core.model.Frame.COMMAND_COMMIT;
import static com.excilys.soja.core.model.Frame.COMMAND_CONNECT;
import static com.excilys.soja.core.model.Frame.COMMAND_CONNECTED;
import static com.excilys.soja.core.model.Frame.COMMAND_DISCONNECT;
import static com.excilys.soja.core.model.Frame.COMMAND_ERROR;
import static com.excilys.soja.core.model.Frame.COMMAND_MESSAGE;
import static com.excilys.soja.core.model.Frame.COMMAND_NACK;
import static com.excilys.soja.core.model.Frame.COMMAND_RECEIPT;
import static com.excilys.soja.core.model.Frame.COMMAND_SEND;
import static com.excilys.soja.core.model.Frame.COMMAND_SUBSCRIBE;
import static com.excilys.soja.core.model.Frame.COMMAND_UNSUBSCRIBE;
import static com.excilys.soja.core.model.Frame.COMMAND_HEARBEAT;

import java.io.IOException;
import java.util.Scanner;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import com.excilys.soja.core.exception.ParseException;
import com.excilys.soja.core.model.Frame;
import com.excilys.soja.core.model.Header;

/**
 * @author dvilleneuve
 * 
 */
public class FrameParser {

	private static final String[] VALID_COMMANDS = { COMMAND_CONNECT, COMMAND_DISCONNECT, COMMAND_SEND,
			COMMAND_MESSAGE, COMMAND_SUBSCRIBE, COMMAND_UNSUBSCRIBE, COMMAND_BEGIN, COMMAND_COMMIT, COMMAND_ABORT,
			COMMAND_RECEIPT, COMMAND_CONNECTED, COMMAND_ERROR, COMMAND_ACK, COMMAND_NACK, COMMAND_HEARBEAT };
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
			if (headerLine.isEmpty()) {
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
