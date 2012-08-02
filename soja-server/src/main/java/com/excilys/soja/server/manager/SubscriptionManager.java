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
package com.excilys.soja.server.manager;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.netty.channel.Channel;

import com.excilys.soja.core.model.Ack;
import com.excilys.soja.server.model.Subscription;

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
	public synchronized Set<Subscription> retrieveSubscriptionsByTopic(String topic) {
		return topicsSubscriptions.get(topic);
	}

	/**
	 * Retrieve all subscriptions of a client. If none, return null.
	 * 
	 * @param clientSessionToken
	 * @return
	 */
	public synchronized Map<Long, Subscription> retrieveSubscriptionsByToken(String clientSessionToken) {
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
	public synchronized Subscription addSubscription(Channel channel, String clientSessionToken, Long subscriptionId,
			String topic, Ack ackMode) {
		if (clientSessionToken == null || clientSessionToken.isEmpty())
			throw new NullPointerException("clientSessionToken can't be null nor empty");
		if (subscriptionId == null)
			throw new NullPointerException("subscriptionId can't be null");
		if (topic == null || clientSessionToken.isEmpty())
			throw new NullPointerException("topic can't be null nor empty");

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
	public synchronized boolean removeSubscription(String clientSessionToken, Long subscriptionId) {
		if (clientSessionToken == null || clientSessionToken.isEmpty())
			return false;
		if (subscriptionId == null)
			return false;

		// Clients subscriptions
		Map<Long, Subscription> clientSubscriptions = retrieveSubscriptionsByToken(clientSessionToken);
		if (clientSubscriptions == null) {
			return false;
		}

		Subscription removedSubscription = clientSubscriptions.remove(subscriptionId);

		// If this client has no more subscriptions, remove his subscription Map to free memory
		if (clientSubscriptions.isEmpty()) {
			clientsSubscriptions.remove(clientSessionToken);
		}

		if (removedSubscription != null) {
			// Topics subscriptions
			Set<Subscription> topicSubscriptions = retrieveSubscriptionsByTopic(removedSubscription.getTopic());
			if (topicSubscriptions != null) {
				topicSubscriptions.remove(removedSubscription);

				// If this topic has no more subscriptions, remove his subscription Set to free memory
				if (topicSubscriptions.isEmpty()) {
					topicsSubscriptions.remove(removedSubscription.getTopic());
				}
			}
		}

		return true;
	}

	/**
	 * Remove all subscribers for the given topic.
	 * 
	 * @param topic
	 */
	public synchronized boolean removeSubscriptions(String clientSessionToken) {
		if (clientSessionToken == null || clientSessionToken.isEmpty())
			return false;

		Map<Long, Subscription> removedClientSubscriptions = clientsSubscriptions.remove(clientSessionToken);
		if (removedClientSubscriptions == null)
			return false;

		// Topics subscriptions
		Collection<Subscription> removedSubscriptions = removedClientSubscriptions.values();
		for (Subscription removedSubscription : removedSubscriptions) {
			Set<Subscription> topicSubscriptions = retrieveSubscriptionsByTopic(removedSubscription.getTopic());
			if (topicSubscriptions != null) {
				topicSubscriptions.remove(removedSubscription);
			}
		}

		return true;
	}

}
