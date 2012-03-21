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
package com.excilys.soja.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.excilys.soja.server.StompServer;
import com.excilys.soja.server.authentication.Authentication;

/**
 * @author dvilleneuve
 * 
 */
public class ServerMain {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServerMain.class);

	public static void main(String[] args) {
		final StompServer server = new StompServer(61626, Authentication.ALLOW_ALL_INSTANCE);
		server.setHeartBeat(5000, 8000);
		server.start();

		// Add a hook on the user interupt event
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				LOGGER.debug("User intercept event catched.");
				server.stop();
			}
		});
	}

}
