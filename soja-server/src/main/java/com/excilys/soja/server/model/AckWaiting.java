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
package com.excilys.soja.server.model;

import java.util.Set;

import org.jboss.netty.channel.Channel;

import com.excilys.soja.core.model.Frame;

/**
 * @author dvilleneuve
 * 
 */
public class AckWaiting {

	private final Channel channel;
	private final Set<Long> subscriptionIds;
	private final Frame sendFrame;

	public AckWaiting(Channel channel, Set<Long> subscriptionIds, Frame sendFrame) {
		this.channel = channel;
		this.subscriptionIds = subscriptionIds;
		this.sendFrame = sendFrame;
	}

	public Set<Long> getSubscriptionIds() {
		return subscriptionIds;
	}

	public Frame getSendFrame() {
		return sendFrame;
	}

	public Channel getChannel() {
		return channel;
	}

	public void removeSubscriptionId(Long subscriptionId) {
		subscriptionIds.remove(subscriptionId);
	}

}
