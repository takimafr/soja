/**
###############################################################################
# Contributors:
#     Damien VILLENEUVE - initial API and implementation
###############################################################################
 */
package com.excilys.soja.utils;

import com.excilys.soja.model.Frame;
import com.excilys.soja.model.Header;

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
