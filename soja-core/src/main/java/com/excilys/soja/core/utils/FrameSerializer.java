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

import com.excilys.soja.core.model.Frame;
import com.excilys.soja.core.model.Header;

/**
 * @author dvilleneuve
 * 
 */
public class FrameSerializer {

	public static String serializeFrame(Frame frame) {
		if (frame == null)
			return null;

		// COMMAND
		StringBuilder formatedFrame = new StringBuilder();
		String command = frame.getCommand();
		if (command != null && command.length() > 0) {
			formatedFrame.append(command).append(Frame.EOL_COMMAND);
		} else {
			return null;
		}

		// HEADER
		Header header = frame.getHeader();
		if (header != null) {
			if (header.size() > 0) {
				for (String key : header.keySet()) {
					formatedFrame.append(key).append(Frame.SEPARATOR_HEADER).append(header.get(key))
							.append(Frame.EOL_HEADER);
				}
				formatedFrame.append(Frame.EOL_HEADERS);
			}
		}

		// BODY
		String body = frame.getBody();
		if (body != null && body.length() > 0) {
			formatedFrame.append(body);
		}

		formatedFrame.append(Frame.EOL_FRAME);
		return formatedFrame.toString();
	}

	public static String escapeHeaderValue(String headerValue) {
		headerValue = headerValue.replace("\n", "\n");
		headerValue = headerValue.replace(":", "\\c");
		headerValue = headerValue.replace("\\", "\\\\");

		return headerValue;
	}

}
