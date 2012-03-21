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

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.excilys.soja.core.model.Frame;


/**
 * @author dvilleneuve
 * 
 */
public abstract class StompHandler extends SimpleChannelHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(StompHandler.class);

	/**
	 * Send a STOMP frame to the remote
	 * 
	 * @param frame
	 */
	public void sendFrame(Channel channel, Frame frame) {
		LOGGER.debug("Sending : {}", frame);
		channel.write(frame);
	}

}
