/**
###############################################################################
# Contributors:
#     Damien VILLENEUVE - initial API and implementation
###############################################################################
 */
package com.excilys.stomp.model;

import com.excilys.stomp.netty.ClientRemoteSession;


/**
 * @author dvilleneuve
 * 
 */
public class Subscription implements Comparable<Subscription> {
	private final ClientRemoteSession clientRemoteSession;
	private final Long subscriptionId;
	private final String topic;
	private final Ack ackMode;

	/**
	 * @param topic
	 * @param ackMode
	 * @param subscriptionId
	 */
	public Subscription(ClientRemoteSession clientRemoteSession, Long subscriptionId, String topic, Ack ackMode) {
		this.clientRemoteSession = clientRemoteSession;
		this.subscriptionId = subscriptionId;
		this.topic = topic;
		this.ackMode = ackMode;
	}

	public ClientRemoteSession getClientRemoteSession() {
		return clientRemoteSession;
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
		result = prime * result + ((clientRemoteSession == null) ? 0 : clientRemoteSession.hashCode());
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
		if (clientRemoteSession == null) {
			if (other.clientRemoteSession != null)
				return false;
		} else if (!clientRemoteSession.equals(other.clientRemoteSession))
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
		String id1 = clientRemoteSession.getSessionToken() + subscriptionId;
		String id2 = o.getClientRemoteSession().getSessionToken() + o.getSubscriptionId();
		return id1.compareTo(id2);
	}
}
