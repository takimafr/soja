/**
###############################################################################
# Contributors:
#     Damien VILLENEUVE - initial API and implementation
###############################################################################
 */
package com.excilys.stomp.netty;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.netty.channel.Channel;

import com.excilys.stomp.model.Ack;
import com.excilys.stomp.model.Subscription;

/**
 * @author dvilleneuve
 * 
 */
public class SubscriptionManager {

	private static final SubscriptionManager instance = new SubscriptionManager();
	private final Map<String, Set<Subscription>> topicsSubscriptions = new HashMap<String, Set<Subscription>>();
	private final Map<String, Map<Long, Subscription>> clientsSubscriptions = new HashMap<String, Map<Long, Subscription>>();

	public static SubscriptionManager getInstance() {
		return instance;
	}

	private SubscriptionManager() {
	}

	/**
	 * Retrieve all subscriptions for a topic. If none has subscribed to this topic, return null.
	 * 
	 * @param topic
	 * @return
	 */
	public Set<Subscription> retrieveSubscriptionsByTopic(String topic) {
		return topicsSubscriptions.get(topic);
	}

	/**
	 * Retrieve all subscriptions of a client. If none, return null.
	 * 
	 * @param clientSessionToken
	 * @return
	 */
	public Map<Long, Subscription> retrieveSubscriptionsByToken(String clientSessionToken) {
		return clientsSubscriptions.get(clientSessionToken);
	}

	/**
	 * Add a subscriber for the given topic. If the client has already subscribed to this topic, leave the subscribers
	 * list unchanged and return false. Else, add the subscriber and return true
	 * 
	 * @param channel
	 * @param clientSessionToken
	 * @param subscriptionId
	 * @param topic
	 * @param ackMode
	 * @return true if the subscriber has beed added, false else (or if he's already added)
	 */
	public Subscription addSubscription(Channel channel, String clientSessionToken, Long subscriptionId, String topic,
			Ack ackMode) {
		Subscription subscription = new Subscription(channel, subscriptionId, topic, ackMode);

		// Clients subscriptions
		Map<Long, Subscription> clientSubscriptions = retrieveSubscriptionsByToken(clientSessionToken);
		if (clientSubscriptions == null) {
			clientSubscriptions = new HashMap<Long, Subscription>();
			clientsSubscriptions.put(clientSessionToken, clientSubscriptions);
		}
		clientSubscriptions.put(subscriptionId, subscription);

		// Topics subscriptions
		Set<Subscription> topicSubscriptions = retrieveSubscriptionsByTopic(topic);
		if (topicSubscriptions == null) {
			topicSubscriptions = new TreeSet<Subscription>();
			topicsSubscriptions.put(topic, topicSubscriptions);
		}
		topicSubscriptions.add(subscription);

		return subscription;
	}

	/**
	 * Remove a subscriber for the given topic.
	 * 
	 * @param clientSessionToken
	 * @param subscriptionId
	 * @return true if the client has subscribed to this topic, false else
	 */
	public boolean removeSubscription(String clientSessionToken, Long subscriptionId) {
		// Clients subscriptions
		Map<Long, Subscription> clientSubscriptions = retrieveSubscriptionsByToken(clientSessionToken);
		if (clientSubscriptions != null) {
			Subscription removedSubscription = clientSubscriptions.remove(subscriptionId);
			if (removedSubscription != null) {
				// If this client has no more subscriptions, remove his subscription Map to free memory
				if (clientSubscriptions.size() == 0) {
					clientsSubscriptions.remove(clientSessionToken);
				}

				// Topics subscriptions
				Set<Subscription> topicSubscriptions = retrieveSubscriptionsByTopic(removedSubscription.getTopic());
				if (topicSubscriptions != null) {
					topicSubscriptions.remove(removedSubscription);
				}

				return true;
			}
		}
		return false;
	}

	/**
	 * Remove all subscribers for the given topic.
	 * 
	 * @param topic
	 */
	public void removeSubscriptions(String clientSessionToken) {
		Map<Long, Subscription> removedClientSubscriptions = clientsSubscriptions.remove(clientSessionToken);

		// Topics subscriptions
		Collection<Subscription> removedSubscriptions = removedClientSubscriptions.values();
		for (Subscription removedSubscription : removedSubscriptions) {
			Set<Subscription> topicSubscriptions = retrieveSubscriptionsByTopic(removedSubscription.getTopic());
			if (topicSubscriptions != null) {
				topicSubscriptions.remove(removedSubscription);
			}
		}
	}

}
