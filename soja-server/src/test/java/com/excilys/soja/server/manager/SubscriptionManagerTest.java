package com.excilys.soja.server.manager;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

import com.excilys.soja.core.model.Ack;
import com.excilys.soja.server.model.Subscription;

public class SubscriptionManagerTest {

	private static final String SESSION = "session";
	private static final Long SUBSCRIPTION_ID = 1L;
	private static final String TOPIC = "/topic";
	private static final Ack ACK = Ack.AUTO;

	private SubscriptionManager subscriptionManager = SubscriptionManager.getInstance();

	@Test(expected = NullPointerException.class)
	public void testAddSubscription_all_null() throws Exception {
		subscriptionManager.addSubscription(null, null, null, null, null);
	}

	@Test(expected = NullPointerException.class)
	public void testAddSubscription_client_session_null() throws Exception {
		subscriptionManager.addSubscription(null, null, SUBSCRIPTION_ID, TOPIC, ACK);
	}

	@Test(expected = NullPointerException.class)
	public void testAddSubscription_subscriptionId_null() throws Exception {
		subscriptionManager.addSubscription(null, SESSION, null, TOPIC, ACK);
	}

	@Test(expected = NullPointerException.class)
	public void testAddSubscription_topic_null() throws Exception {
		subscriptionManager.addSubscription(null, SESSION, SUBSCRIPTION_ID, null, ACK);
	}

	@Test
	public void testAddSubscription() throws Exception {
		Subscription expectedSubscription = new Subscription(null, SUBSCRIPTION_ID, TOPIC, ACK);
		Subscription addedSubscription = subscriptionManager
				.addSubscription(null, SESSION, SUBSCRIPTION_ID, TOPIC, ACK);

		assertEquals(expectedSubscription, addedSubscription);
	}

	@Test
	public void testRetrieveSubscriptionsByTopic_null() throws Exception {
		assertNull(subscriptionManager.retrieveSubscriptionsByToken(null));
	}

	@Test
	public void testRetrieveSubscriptionsByTopic_not_found() throws Exception {
		assertNull(subscriptionManager.retrieveSubscriptionsByToken("unknown"));
	}

	@Test
	public void testRetrieveSubscriptionsByTopic() throws Exception {
		Subscription expectedSubscription = new Subscription(null, SUBSCRIPTION_ID, TOPIC, ACK);
		Map<Long, Subscription> expectedSubscriptions = new HashMap<Long, Subscription>();
		expectedSubscriptions.put(SUBSCRIPTION_ID, expectedSubscription);

		Map<Long, Subscription> subscriptions = subscriptionManager.retrieveSubscriptionsByToken(SESSION);

		assertTrue(subscriptions.size() > 0);
		assertEquals(expectedSubscriptions, subscriptions);
	}

	@Test
	public void testRetrieveSubscriptionsByToken_null() throws Exception {
		assertNull(subscriptionManager.retrieveSubscriptionsByTopic(null));
	}

	@Test
	public void testRetrieveSubscriptionsByToken_not_found() throws Exception {
		assertNull(subscriptionManager.retrieveSubscriptionsByTopic("unknown"));
	}

	@Test
	public void testRetrieveSubscriptionsByToken() throws Exception {
		Subscription expectedSubscription = new Subscription(null, SUBSCRIPTION_ID, TOPIC, ACK);
		Set<Subscription> expectedSubscriptions = new TreeSet<Subscription>();
		expectedSubscriptions.add(expectedSubscription);

		Set<Subscription> subscriptions = subscriptionManager.retrieveSubscriptionsByTopic(TOPIC);

		assertTrue(subscriptions.size() > 0);
		assertEquals(expectedSubscriptions, subscriptions);
	}

	@Test
	public void testRemoveSubscription_client_session_null() throws Exception {
		assertFalse(subscriptionManager.removeSubscription(null, SUBSCRIPTION_ID));
	}

	@Test
	public void testRemoveSubscription_subscriptionId_null() throws Exception {
		assertFalse(subscriptionManager.removeSubscription(SESSION, null));
	}

	@Test
	public void testRemoveSubscription() throws Exception {
		subscriptionManager.addSubscription(null, SESSION, SUBSCRIPTION_ID, TOPIC, ACK);

		Map<Long, Subscription> subscriptionsBySession = subscriptionManager.retrieveSubscriptionsByToken(SESSION);
		Set<Subscription> subscriptionsByTopic = subscriptionManager.retrieveSubscriptionsByTopic(TOPIC);
		assertTrue(subscriptionsBySession.size() == 1);
		assertTrue(subscriptionsByTopic.size() == 1);

		assertTrue(subscriptionManager.removeSubscription(SESSION, SUBSCRIPTION_ID));

		assertNull(subscriptionManager.retrieveSubscriptionsByToken(SESSION));
		assertNull(subscriptionManager.retrieveSubscriptionsByTopic(TOPIC));
	}

}
