/**
###############################################################################
# Contributors:
#     Damien VILLENEUVE - initial API and implementation
###############################################################################
 */
package com.excilys.stomp.model;

import java.util.Set;

import com.excilys.stomp.netty.ClientRemoteSession;

/**
 * @author dvilleneuve
 * 
 */
public class AckWaiting {

	private final Set<Long> subscriptionIds;
	private final Frame sendFrame;
	private final ClientRemoteSession sendClientSession;

	public AckWaiting(Set<Long> subscriptionIds, Frame sendFrame, ClientRemoteSession sendClientSession) {
		this.subscriptionIds = subscriptionIds;
		this.sendFrame = sendFrame;
		this.sendClientSession = sendClientSession;
	}

	public Set<Long> getSubscriptionIds() {
		return subscriptionIds;
	}

	public Frame getSendFrame() {
		return sendFrame;
	}

	public ClientRemoteSession getSendClientSession() {
		return sendClientSession;
	}

	public void removeSubscriptionId(Long subscriptionId) {
		subscriptionIds.remove(subscriptionId);
	}

}
