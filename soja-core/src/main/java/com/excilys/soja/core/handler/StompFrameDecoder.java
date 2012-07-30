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
package com.excilys.soja.core.handler;

import static com.excilys.soja.core.model.Frame.COMMAND_ABORT;
import static com.excilys.soja.core.model.Frame.COMMAND_ACK;
import static com.excilys.soja.core.model.Frame.COMMAND_BEGIN;
import static com.excilys.soja.core.model.Frame.COMMAND_COMMIT;
import static com.excilys.soja.core.model.Frame.COMMAND_CONNECT;
import static com.excilys.soja.core.model.Frame.COMMAND_CONNECTED;
import static com.excilys.soja.core.model.Frame.COMMAND_DISCONNECT;
import static com.excilys.soja.core.model.Frame.COMMAND_ERROR;
import static com.excilys.soja.core.model.Frame.COMMAND_HEARBEAT;
import static com.excilys.soja.core.model.Frame.COMMAND_MESSAGE;
import static com.excilys.soja.core.model.Frame.COMMAND_NACK;
import static com.excilys.soja.core.model.Frame.COMMAND_RECEIPT;
import static com.excilys.soja.core.model.Frame.COMMAND_SEND;
import static com.excilys.soja.core.model.Frame.COMMAND_SUBSCRIBE;
import static com.excilys.soja.core.model.Frame.COMMAND_UNSUBSCRIBE;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

import com.excilys.soja.core.exception.ParseException;
import com.excilys.soja.core.model.Frame;
import com.excilys.soja.core.model.Header;

/**
 * @author dvilleneuve
 * 
 */
public class StompFrameDecoder extends FrameDecoder {

	private static final String[] VALID_COMMANDS = { COMMAND_CONNECT, COMMAND_DISCONNECT, COMMAND_SEND,
			COMMAND_MESSAGE, COMMAND_SUBSCRIBE, COMMAND_UNSUBSCRIBE, COMMAND_BEGIN, COMMAND_COMMIT, COMMAND_ABORT,
			COMMAND_RECEIPT, COMMAND_CONNECTED, COMMAND_ERROR, COMMAND_ACK, COMMAND_NACK, COMMAND_HEARBEAT };
	private static final String[] COMMANDS_WITH_BODY = { COMMAND_SEND, COMMAND_MESSAGE, COMMAND_ERROR };

	private Frame currentFrame;

	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {
		// COMMAND
		String command = readString(buffer, Frame.EOL_COMMAND);
		if (command == null)
			return null;

		if (isValidCommand(command)) {
			currentFrame = new Frame(command, new Header(), null);
		} else {
			if (currentFrame == null) {
				throw new ParseException(command + " is not a valid command");
			} else {
				String oldBody = currentFrame.getBody();

				long contentLength = 0;
				String contentLengthString = currentFrame.getHeaderValue(Header.HEADER_CONTENT_LENGTH);
				if (contentLengthString != null) {
					contentLength = Long.parseLong(contentLengthString);
				}

				String bodyPart = null;
				while ((bodyPart = readString(buffer, Frame.EOL_FRAME)) != null) {
					oldBody += bodyPart + Frame.EOL_FRAME;
				}
				currentFrame.setBody(StringUtils.chop(oldBody));

				System.out.println(oldBody.length() + buffer.readableBytes());

				if ((oldBody.length() + buffer.readableBytes()) < contentLength) {
					// Ask for more data because there are not enough according to content-length header
					return null;
				} else {
					Frame frame = currentFrame;
					currentFrame = null;
					return frame;
				}
			}
		}

		// HEADER
		String headerLine = null;
		while ((headerLine = readString(buffer, Frame.EOL_HEADER)) != null) {
			if (headerLine == null || headerLine.isEmpty()) {
				break;
			}

			String[] headerParts = headerLine.split(":");
			if (headerParts.length == 2) {
				currentFrame.setHeaderValue(headerParts[0], unescapeHeaderValue(headerParts[1]));
			}
		}

		// BODY
		if (isExpectedBody(command)) {
			long contentLength = 0;
			String contentLengthString = currentFrame.getHeaderValue(Header.HEADER_CONTENT_LENGTH);
			if (contentLengthString != null) {
				contentLength = Long.parseLong(contentLengthString);
			}

			int currentBodyLength = buffer.readableBytes();

			StringBuilder body = new StringBuilder();
			String bodyPart = null;

			while ((bodyPart = readString(buffer, Frame.EOL_FRAME)) != null) {
				body.append(bodyPart).append(Frame.EOL_FRAME);
			}
			currentFrame.setBody(StringUtils.chop(body.toString()));

			if (currentBodyLength < contentLength) {
				// Ask for more data because there are not enough according to content-length header
				return null;
			}
		} else {
			if (currentFrame.getHeader().size() > 0) {
				buffer.skipBytes(1); // Skip the headers separator
			}
		}

		Frame frame = currentFrame;
		currentFrame = null;
		return frame;
	}

	private String readString(ChannelBuffer buffer, char delimiter) {
		int bytesBefore = buffer.bytesBefore((byte) delimiter);
		if (bytesBefore == -1)
			return null;

		byte[] value = new byte[bytesBefore];
		buffer.readBytes(value);
		buffer.skipBytes(1); // Skip the delimiter
		return new String(value);
	}

	private boolean isValidCommand(String command) {
		return command != null && ArrayUtils.contains(VALID_COMMANDS, command);
	}

	private boolean isExpectedBody(String command) {
		return command != null && ArrayUtils.contains(COMMANDS_WITH_BODY, command);
	}

	private String unescapeHeaderValue(String headerValue) {
		headerValue = headerValue.replace("\\n", "\n");
		headerValue = headerValue.replace("\\c", ":");
		headerValue = headerValue.replace("\\\\", "\\");

		return headerValue;
	}

}
