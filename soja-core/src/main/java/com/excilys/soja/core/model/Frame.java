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
package com.excilys.soja.core.model;

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
	public static final String COMMAND_ACK = "ACK";
	public static final String COMMAND_NACK = "NACK";

	public static final String COMMAND_RECEIPT = "RECEIPT";
	public static final String COMMAND_CONNECTED = "CONNECTED";
	public static final String COMMAND_ERROR = "ERROR";

	public static final String COMMAND_HEARBEAT = "HEARTBEAT";
	
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

	public void setHeaderValue(String key, String value) {
		header.set(key, value);
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
