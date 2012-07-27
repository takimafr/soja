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

import static com.excilys.soja.core.model.Header.HEADER_HEART_BEAT;

import com.excilys.soja.core.model.Frame;
import com.excilys.soja.core.model.Header;

/**
 * @author dvilleneuve
 * 
 */
public class ConnectedFrame extends Frame {

	public ConnectedFrame(String version) {
		super(Frame.COMMAND_CONNECTED, new Header().set(Header.HEADER_VERSION, version), null);
	}

	public ConnectedFrame setSession(String session) {
		setHeaderValue(Header.HEADER_SESSION, session);
		return this;
	}

	public ConnectedFrame setServerName(String serverName) {
		setHeaderValue(Header.HEADER_SERVER, serverName);
		return this;
	}

	public void setHeartBeat(long localGuaranteedHeartBeat, long localExpectedHeartBeat) {
		setHeaderValue(HEADER_HEART_BEAT, localGuaranteedHeartBeat + "," + localExpectedHeartBeat);
	}

}
