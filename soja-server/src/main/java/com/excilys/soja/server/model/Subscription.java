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

import org.jboss.netty.channel.Channel;

import com.excilys.soja.core.model.Ack;


/**
 * @author dvilleneuve
 * 
 */
public class Subscription implements Comparable<Subscription> {
	private final Channel channel;
	private final Long subscriptionId;
	private final String topic;
	private final Ack ackMode;

	/**
	 * @param topic
	 * @param ackMode
	 * @param subscriptionId
	 */
	public Subscription(Channel channel, Long subscriptionId, String topic, Ack ackMode) {
		this.channel = channel;
		this.subscriptionId = subscriptionId;
		this.topic = topic;
		this.ackMode = ackMode;
	}

	public Channel getChannel() {
		return channel;
	}

	public Long getSubscriptionId() {
		return subscriptionId;
	}

	public String getTopic() {
		return topic;
	}

	public Ack getAckMode() {
		return ackMode;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ackMode == null) ? 0 : ackMode.hashCode());
		result = prime * result + ((channel == null) ? 0 : channel.hashCode());
		result = prime * result + ((subscriptionId == null) ? 0 : subscriptionId.hashCode());
		result = prime * result + ((topic == null) ? 0 : topic.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Subscription other = (Subscription) obj;
		if (ackMode != other.ackMode)
			return false;
		if (channel == null) {
			if (other.channel != null)
				return false;
		} else if (!channel.equals(other.channel))
			return false;
		if (subscriptionId == null) {
			if (other.subscriptionId != null)
				return false;
		} else if (!subscriptionId.equals(other.subscriptionId))
			return false;
		if (topic == null) {
			if (other.topic != null)
				return false;
		} else if (!topic.equals(other.topic))
			return false;
		return true;
	}

	@Override
	public int compareTo(Subscription o) {
		String id1 = channel.getId().toString() + subscriptionId;
		String id2 = o.getChannel().getId().toString() + o.getSubscriptionId();
		return id1.compareTo(id2);
	}
}
