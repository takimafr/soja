/**
###############################################################################
# Contributors:
#     Damien VILLENEUVE - initial API and implementation
###############################################################################
 */
package com.excilys.stomp.netty;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author dvilleneuve
 * 
 */
public class SubscriptionManager {

	private static final SubscriptionManager instance = new SubscriptionManager();
	private final Map<String, Set<ClientRemoteSession>> subscriptions = new HashMap<String, Set<ClientRemoteSession>>();

	public static SubscriptionManager getInstance() {
		return instance;
	}

	private SubscriptionManager() {
	}

	/**
	 * Retrieve all subscribers for a topic. If none has subscribed to this topic, return null.
	 * 
	 * @param topic
	 * @return
	 */
	public Set<ClientRemoteSession> retrieveSubscribers(String topic) {
		return subscriptions.get(topic);
	}

	/**
	 * Add a subscriber for the given topic. If the client has already subscribed to this topic, leave the subscribers
	 * list unchanged and return false. Else, add the subscriber and return true
	 * 
	 * @param topic
	 * @param clientSession
	 * @return true if the subscriber has beed added, false else (or if he's already added)
	 */
	public boolean addSubscriber(String topic, ClientRemoteSession clientSession) {
		Set<ClientRemoteSession> retrieveSubscribers = retrieveSubscribers(topic);
		if (retrieveSubscribers == null) {
			retrieveSubscribers = new TreeSet<ClientRemoteSession>();
			subscriptions.put(topic, retrieveSubscribers);
		}
		return retrieveSubscribers.add(clientSession);
	}

	/**
	 * Remove a subscriber for the given topic.
	 * 
	 * @param topic
	 * @param clientSession
	 * @return true if the client has subscribed to this topic, false else
	 */
	public boolean removeSubscriber(String topic, ClientRemoteSession clientSession) {
		Set<ClientRemoteSession> retrieveSubscribers = retrieveSubscribers(topic);
		if (retrieveSubscribers != null) {
			if (retrieveSubscribers.remove(clientSession)) {
				if (retrieveSubscribers.size() == 0) {
					subscriptions.remove(topic);
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Remove all subscribers for the given topic.
	 * 
	 * @param topic
	 */
	public void removeSubscribers(String topic) {
		subscriptions.remove(topic);
	}
}
