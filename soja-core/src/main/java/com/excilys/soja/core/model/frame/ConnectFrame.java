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
package com.excilys.soja.core.model.frame;

import com.excilys.soja.core.model.Frame;
import com.excilys.soja.core.model.Header;

/**
 * @author dvilleneuve
 * 
 */
public class ConnectFrame extends Frame {

	public ConnectFrame(String acceptVersion, String hostname) {
		super(Frame.COMMAND_CONNECT, new Header().set(Header.HEADER_ACCEPT_VERSION, acceptVersion).set(Header.HEADER_HOST, hostname), null);
	}

	public ConnectFrame(String acceptVersion, String hostname, String login, String password) {
		this(acceptVersion, hostname);
		getHeader().set(Header.HEADER_LOGIN, login);
		getHeader().set(Header.HEADER_PASSCODE, password);
	}

}
