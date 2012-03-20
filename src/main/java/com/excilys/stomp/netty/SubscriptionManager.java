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

import com.excilys.stomp.model.Ack;
import com.excilys.stomp.model.Subscription;

/**
 * @author dvilleneuve
 * 
 */
public class SubscriptionManager {

	private static final SubscriptionManager instance = new SubscriptionManager();
	private final Map<String, Set<Subscription>> topicsSubscriptions = new HashMap<String, Set<Subscription>>();
	private final Map<ClientRemoteSession, Map<Long, Subscription>> clientsSubscriptions = new HashMap<ClientRemoteSession, Map<Long, Subscription>>();

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
	public Set<Subscription> retrieveSubscriptions(String topic) {
		return topicsSubscriptions.get(topic);
	}

	/**
	 * Retrieve all subscriptions of a client. If none, return null.
	 * 
	 * @param clientRemoteSession
	 * @return
	 */
	public Map<Long, Subscription> retrieveSubscriptions(ClientRemoteSession clientRemoteSession) {
		return clientsSubscriptions.get(clientRemoteSession);
	}

	/**
	 * Add a subscriber for the given topic. If the client has already subscribed to this topic, leave the subscribers
	 * list unchanged and return false. Else, add the subscriber and return true
	 * 
	 * @param clientRemoteSession
	 * @param subscriptionId
	 * @param topic
	 * @param ackMode
	 * @return true if the subscriber has beed added, false else (or if he's already added)
	 */
	public Subscription addSubscription(ClientRemoteSession clientRemoteSession, Long subscriptionId, String topic,
			Ack ackMode) {
		Subscription subscription = new Subscription(clientRemoteSession, subscriptionId, topic, ackMode);

		// Clients subscriptions
		Map<Long, Subscription> clientSubscriptions = retrieveSubscriptions(clientRemoteSession);
		if (clientSubscriptions == null) {
			clientSubscriptions = new HashMap<Long, Subscription>();
			clientsSubscriptions.put(clientRemoteSession, clientSubscriptions);
		}
		clientSubscriptions.put(subscriptionId, subscription);

		// Topics subscriptions
		Set<Subscription> topicSubscriptions = retrieveSubscriptions(topic);
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
	 * @param clientRemoteSession
	 * @param subscriptionId
	 * @return true if the client has subscribed to this topic, false else
	 */
	public boolean removeSubscription(ClientRemoteSession clientRemoteSession, Long subscriptionId) {
		// Clients subscriptions
		Map<Long, Subscription> clientSubscriptions = retrieveSubscriptions(clientRemoteSession);
		if (clientSubscriptions != null) {
			Subscription removedSubscription = clientSubscriptions.remove(subscriptionId);
			if (removedSubscription != null) {
				// If this client has no more subscriptions, remove his subscription Map to free memory
				if (clientSubscriptions.size() == 0) {
					clientsSubscriptions.remove(clientRemoteSession);
				}

				// Topics subscriptions
				Set<Subscription> topicSubscriptions = retrieveSubscriptions(removedSubscription.getTopic());
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
	public void removeSubscriptions(ClientRemoteSession clientRemoteSession) {
		Map<Long, Subscription> removedClientSubscriptions = clientsSubscriptions.remove(clientRemoteSession);

		// Topics subscriptions
		Collection<Subscription> removedSubscriptions = removedClientSubscriptions.values();
		for (Subscription removedSubscription : removedSubscriptions) {
			Set<Subscription> topicSubscriptions = retrieveSubscriptions(removedSubscription.getTopic());
			if (topicSubscriptions != null) {
				topicSubscriptions.remove(removedSubscription);
			}
		}
	}

}
