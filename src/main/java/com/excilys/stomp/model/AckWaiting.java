/**
###############################################################################
# Contributors:
#     Damien VILLENEUVE - initial API and implementation
###############################################################################
 */
package com.excilys.stomp.model;

import java.util.Set;

import org.jboss.netty.channel.Channel;

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
