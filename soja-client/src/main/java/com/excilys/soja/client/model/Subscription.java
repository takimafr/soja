/**
###############################################################################
# Contributors:
#     Damien VILLENEUVE - initial API and implementation
###############################################################################
 */
package com.excilys.soja.client.model;

import com.excilys.soja.client.events.StompTopicListener;
import com.excilys.soja.core.model.Ack;

/**
 * @author dvilleneuve
 * 
 */
public class Subscription {

	private final Long id;
	private final Ack ackMode;
	private final String topic;
	private final StompTopicListener topicListener;

	/**
	 * @param id
	 *            unique subscription id
	 * @param ackMode
	 * @param topic
	 *            the topic for wich the subscription is for
	 * @param topicListener
	 *            the topic listener to call when the client received a message
	 */
	public Subscription(Long id, Ack ackMode, String topic, StompTopicListener topicListener) {
		super();
		this.id = id;
		this.ackMode = ackMode;
		this.topic = topic;
		this.topicListener = topicListener;
	}

	public Long getId() {
		return id;
	}

	public Ack getAckMode() {
		return ackMode;
	}

	public String getTopic() {
		return topic;
	}

	public StompTopicListener getTopicListener() {
		return topicListener;
	}

}
