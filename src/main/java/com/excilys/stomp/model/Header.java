/**
###############################################################################
# Contributors:
#     Damien VILLENEUVE - initial API and implementation
###############################################################################
 */
package com.excilys.stomp.model;

import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author dvilleneuve
 * 
 */
public class Header extends HashMap<String, String> {
	private static final long serialVersionUID = 776915964777745604L;

	public static final String HEADER_ACCEPT_VERSION = "accept-version";
	public static final String HEADER_HOST = "host";
	public static final String HEADER_LOGIN = "login";
	public static final String HEADER_PASSCODE = "passcode";

	public static final String HEADER_VERSION = "version";
	public static final String HEADER_SESSION = "session";
	public static final String HEADER_SERVER = "server";

	public static final String HEADER_ACK = "ack";
	public static final String HEADER_RECEIPT_ID_REQUEST = "receipt";
	public static final String HEADER_RECEIPT_ID_RESPONSE = "receipt-id";

	public static final String HEADER_MESSAGE = "message";

	public static final String HEADER_DESTINATION = "destination";
	public static final String HEADER_MESSAGE_ID = "message-id";
	public static final String HEADER_CONTENT_TYPE = "content-type";
	public static final String HEADER_CONTENT_LENGTH = "content-length";
	public static final String HEADER_TRANSACTION = "transaction";
	
	public static final String HEADER_SUBSCRIPTION_ID = "id";
	public static final String HEADER_SUBSCRIPTION = "subscription";

	public Header() {
	}

	public Header(String[] keys, String[] values) {
		if (keys.length != values.length) {
			throw new IllegalArgumentException("The number of keys must be the same as the number of values");
		}

		for (int i = 0; i < keys.length; i++) {
			put(keys[i], values[i]);
		}
	}

	public String get(String key, String defaultValue) {
		String value = get(key);
		if (value == null) {
			return defaultValue;
		}
		return value;
	}

	public Header set(String key, String value) {
		if (key != null && value != null) {
			put(key, value);
		}
		return this;
	}
	
	public Set<String> allKeys(String[] filters) {
		TreeSet<String> filteredKeys = new TreeSet<String>(keySet());
		for (String filter : filters) {
			filteredKeys.remove(filter);
		}
		return filteredKeys;
	}
}
