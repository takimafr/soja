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
import static com.excilys.soja.core.model.Frame.COMMAND_MESSAGE;
import static com.excilys.soja.core.model.Frame.COMMAND_NACK;
import static com.excilys.soja.core.model.Frame.COMMAND_RECEIPT;
import static com.excilys.soja.core.model.Frame.COMMAND_SEND;
import static com.excilys.soja.core.model.Frame.COMMAND_SUBSCRIBE;
import static com.excilys.soja.core.model.Frame.COMMAND_UNSUBSCRIBE;

import org.apache.commons.lang.ArrayUtils;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.replay.ReplayingDecoder;
import org.jboss.netty.handler.codec.replay.VoidEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.excilys.soja.core.model.Frame;
import com.excilys.soja.core.model.Header;
import com.excilys.soja.core.model.frame.HeartBeatFrame;

/**
 * @author dvilleneuve
 * 
 */
public class StompFrameDecoder extends ReplayingDecoder<VoidEnum> {

	private static final Logger LOGGER = LoggerFactory.getLogger(StompFrameDecoder.class);

	private static final String[] VALID_COMMANDS = { COMMAND_CONNECT, COMMAND_DISCONNECT, COMMAND_SEND,
			COMMAND_MESSAGE, COMMAND_SUBSCRIBE, COMMAND_UNSUBSCRIBE, COMMAND_BEGIN, COMMAND_COMMIT, COMMAND_ABORT,
			COMMAND_RECEIPT, COMMAND_CONNECTED, COMMAND_ERROR, COMMAND_ACK, COMMAND_NACK };
	private static final String[] COMMANDS_WITH_BODY = { COMMAND_SEND, COMMAND_MESSAGE, COMMAND_ERROR };

	public StompFrameDecoder() {
		super(true);
	}

	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer, VoidEnum state)
			throws Exception {
		// If the stream is just a new line, return an heart beat frame
		if (buffer.readableBytes() > 0 && buffer.bytesBefore((byte) '\n') == 0) {
			buffer.readByte();
			return new HeartBeatFrame();
		}

		Frame currentFrame = null;

		// COMMAND
		String command = readString(buffer, Frame.EOL_COMMAND);
		if (isValidCommand(command)) {
			currentFrame = new Frame(command, new Header(), null);
		} else {
			LOGGER.trace("Invalid command : {}", command);
			return null;
		}

		// HEADERS
		String headerLine = null;
		while ((headerLine = readString(buffer, Frame.EOL_HEADER)) != null) {
			String[] headerParts = headerLine.split(":");
			if (headerParts.length == 2) {
				currentFrame.setHeaderValue(unescapeHeader(headerParts[0]), unescapeHeader(headerParts[1]));
			}
		}
		buffer.readByte();

		// BODY
		if (isExpectedBody(currentFrame.getCommand())) {
			String body = null;

			String contentLengthString = currentFrame.getHeaderValue(Header.HEADER_CONTENT_LENGTH);
			if (contentLengthString != null && Long.parseLong(contentLengthString) > 0) {
				int contentLength = Integer.parseInt(contentLengthString);

				body = readString(buffer, contentLength);
				buffer.readByte();
			} else {
				body = readString(buffer, Frame.EOL_FRAME);
			}

			if (body != null && !body.isEmpty()) {
				currentFrame.setBody(body);
			}
		} else {
			buffer.readByte(); // NULL at end of frame
		}

		return currentFrame;

	}

	private String readString(ChannelBuffer buffer, char delimiter) {
		int bytesBefore = buffer.bytesBefore((byte) delimiter);
		if (bytesBefore <= 0)
			return null;

		String value = readString(buffer, bytesBefore);
		buffer.skipBytes(1); // Skip the delimiter
		return value;
	}

	private String readString(ChannelBuffer buffer, int length) {
		byte[] value = new byte[length];
		buffer.readBytes(value);
		return new String(value);
	}

	private boolean isValidCommand(String command) {
		return command != null && ArrayUtils.contains(VALID_COMMANDS, command);
	}

	private boolean isExpectedBody(String command) {
		return command != null && ArrayUtils.contains(COMMANDS_WITH_BODY, command);
	}

	public static String unescapeHeader(String string) {
		if (string == null)
			return string;

		string = string.replace("\\n", "\n");
		string = string.replace("\\c", ":");
		string = string.replace("\\\\", "\\");

		return string;
	}

}
