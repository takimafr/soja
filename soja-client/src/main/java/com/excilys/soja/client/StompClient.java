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
package com.excilys.soja.client;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.excilys.soja.client.events.StompClientListener;
import com.excilys.soja.client.events.StompMessageStateCallback;
import com.excilys.soja.client.events.StompTopicListener;
import com.excilys.soja.client.exception.NotConnectedException;
import com.excilys.soja.client.handler.ClientHandler;
import com.excilys.soja.core.factory.StompPipelineFactory;
import com.excilys.soja.core.model.Ack;
import com.excilys.soja.core.model.frame.DisconnectFrame;

/**
 * @author dvilleneuve
 * 
 */
public class StompClient {

	public static final String SUPPORTED_STOMP_VERSION = "1.1";

	private static final Logger LOGGER = LoggerFactory.getLogger(StompClient.class);

	private static final long CONNECT_TIMEOUT_DEFAULT = 30000;

	private final String hostname;
	private final int port;
	private final ClientBootstrap clientBootstrap;
	private final ClientHandler clientHandler;

	private Channel channel;

	public StompClient(final String hostname, final int port) {
		this.hostname = hostname;
		this.port = port;
		this.clientHandler = new ClientHandler();

		this.clientBootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(Executors.newCachedThreadPool(),
				Executors.newCachedThreadPool()));
		this.clientBootstrap.setPipelineFactory(new StompPipelineFactory() {
			@Override
			public ChannelPipeline getPipeline() throws Exception {
				ChannelPipeline pipeline = super.getPipeline();
				pipeline.addLast("handler", clientHandler);
				return pipeline;
			}
		});
		this.clientBootstrap.setOption("tcpNoDelay", true);
		this.clientBootstrap.setOption("keepAlive", true);
	}

	/**
	 * Connect to the server as a guest (username and password will be <code>null</code>) with default timeout value
	 * (30s)
	 * <p/>
	 * 
	 * @throws SocketException
	 * @throws TimeoutException
	 * @see {@link #connect(String, String, long)}
	 */
	public void connect() throws SocketException, TimeoutException {
		connect(null, null, CONNECT_TIMEOUT_DEFAULT);
	}

	/**
	 * Connect and login to the server with default timeout value. Once the network connection is up the STOMP shakehand
	 * phase will begin.
	 * 
	 * @param username
	 * @param password
	 * @throws SocketException
	 * @throws TimeoutException
	 * @see {@link #connect(String, String, long)}
	 */
	public void connect(String username, String password) throws SocketException, TimeoutException {
		connect(username, password, CONNECT_TIMEOUT_DEFAULT);
	}

	/**
	 * Connect and login to the server with a timeout value. Once the network connection is up the STOMP shakehand phase
	 * will begin.
	 * 
	 * @param username
	 * @param password
	 * @param connectTimeout
	 * @throws SocketException
	 * @throws TimeoutException
	 */
	public void connect(String username, String password, long connectTimeout) throws SocketException, TimeoutException {
		// Start the connection attempt
		ChannelFuture channelFuture = clientBootstrap.connect(new InetSocketAddress(hostname, port));
		if (!channelFuture.awaitUninterruptibly(connectTimeout)) {
			clientBootstrap.releaseExternalResources();
			throw new TimeoutException("Connection timeout to server " + hostname + ":" + port);
		}

		// After waiting until the connection attempt, test if succeeds or fails.
		if (!channelFuture.isSuccess()) {
			clientBootstrap.releaseExternalResources();
			throw new ConnectException("Client failed to connect to " + hostname + ":" + port);
		}

		channel = channelFuture.getChannel();
		if (channel.isConnected()) {
			LOGGER.debug("Connected to {}:{}. Login with username {}...", new Object[] { hostname, port, username });

			// When connected, send a CONNECT command
			clientHandler.connect(channel, SUPPORTED_STOMP_VERSION, hostname, username, password);
		}
	}

	/**
	 * Silently disconnect the STOMP client from the server
	 * 
	 * @throws SocketException
	 * @throws NotConnectedException
	 * @see {@link #disconnect(StompMessageStateCallback)}
	 */
	public void disconnect() throws NotConnectedException, SocketException {
		disconnect(null);
	}

	/**
	 * Disconnect the STOMP client from the server and call back the {@link StompMessageStateCallback#receiptReceived()}
	 * method when the receipt is received.
	 * 
	 * @throws SocketException
	 * @throws NotConnectedException
	 */
	public void disconnect(StompMessageStateCallback callback) throws NotConnectedException, SocketException {
		LOGGER.debug("Disconnecting...");

		ChannelFuture channelFuture = clientHandler.sendFrame(channel, new DisconnectFrame(), callback);
		channelFuture.awaitUninterruptibly();

		if (channel != null && channel.isOpen()) {
			// Close the connection. Make sure the close operation ends because
			// all I/O operations are asynchronous in Netty.
			channel.close().awaitUninterruptibly();
		}

		if (clientBootstrap != null) {
			// Shut down all thread pools to exit.
			clientBootstrap.releaseExternalResources();
		}

		LOGGER.debug("Disconnected");
	}

	/**
	 * Subscribe to the topic named <code>topic</code> and register a {@link StompTopicListener} instance to it. When a
	 * message is received on this topic the {@link StompTopicListener#receivedMessage(String, Map)} method will be
	 * called.
	 * 
	 * @param topic
	 * @param topicListener
	 * @return
	 * @throws NotConnectedException
	 * @throws SocketException
	 * @see {@link #subscribe(String, StompTopicListener, StompMessageStateCallback, Ack)}
	 */
	public Long subscribe(String topic, StompTopicListener topicListener) throws NotConnectedException, SocketException {
		return subscribe(topic, topicListener, null, Ack.AUTO);
	}

	/**
	 * Subscribe to the topic named <code>topic</code> and register a {@link StompTopicListener} instance to it. When a
	 * message is received on this topic the {@link StompTopicListener#receivedMessage(String, Map)} method will be
	 * called.
	 * <p />
	 * This method allows the client to ask for the server a receipt for each frame he sent. When a receipt arrived the
	 * {@link StompMessageStateCallback#receiptReceived()} method will be called. This receipt is used to notify the
	 * submitter than his message has been treated.
	 * <p />
	 * Therefore, the {@link Ack} attribute is used to configure if the client will send an ACK frame each time he
	 * received a message on this topic. This frame is used by the server to know if all clients received a message and
	 * then send back a receipt to the submitter.
	 * 
	 * @param topic
	 * @param topicListener
	 * @return
	 * @throws NotConnectedException
	 * @throws SocketException
	 */
	public Long subscribe(String topic, StompTopicListener topicListener, StompMessageStateCallback callback,
			Ack ackMode) throws NotConnectedException, SocketException {
		return clientHandler.subscribe(channel, topic, topicListener, callback, ackMode);
	}

	/**
	 * Unsubscribe the client from a topic whith the <code>subscriptionId</code> send back by subscribing methods.
	 * 
	 * @param subscriptionId
	 * @throws NotConnectedException
	 * @throws SocketException
	 * @see {@link #unsubscribe(Long, StompMessageStateCallback)}
	 */
	public void unsubscribe(Long subscriptionId) throws NotConnectedException, SocketException {
		unsubscribe(subscriptionId, null);
	}

	/**
	 * Unsubscribe the client from a topic whith the <code>subscriptionId</code> send back by subscribing methods.
	 * <p />
	 * This method allows the client to ask for the server a receipt for this frame. When a receipt arrived the
	 * {@link StompMessageStateCallback#receiptReceived()} method will be called. This receipt is used to notify the
	 * submitter than his message has been treated.
	 * 
	 * @param subscriptionId
	 * @throws NotConnectedException
	 * @throws SocketException
	 * @see {@link #unsubscribe(Long, StompMessageStateCallback)}
	 */
	public void unsubscribe(Long subscriptionId, StompMessageStateCallback callback) throws NotConnectedException,
			SocketException {
		clientHandler.unsubscribe(channel, subscriptionId, callback);
	}

	/**
	 * Send the message <code>message</code> to the topic <code>topic</code>.
	 * 
	 * @param topic
	 * @param message
	 * @throws NotConnectedException
	 * @throws SocketException
	 * @see {@link #send(String, String, Map, StompMessageStateCallback)}
	 */
	public void send(String topic, String message) throws NotConnectedException, SocketException {
		send(topic, message, null, null);
	}

	/**
	 * Send the message <code>message</code> to the topic <code>topic</code>.
	 * <p />
	 * This method allows the client to ask for the server a receipt for this frame. When a receipt arrived the
	 * {@link StompMessageStateCallback#receiptReceived()} method will be called. This receipt is used to notify the
	 * submitter than his message has been treated.
	 * 
	 * @param topic
	 * @param message
	 * @throws NotConnectedException
	 * @throws SocketException
	 * @see {@link #send(String, String, Map, StompMessageStateCallback)}
	 */
	public void send(String topic, String message, StompMessageStateCallback callback) throws NotConnectedException,
			SocketException {
		send(topic, message, null, callback);
	}

	/**
	 * Send the message <code>message</code> to the topic <code>topic</code> with additional headers.
	 * <p />
	 * This method allows the client to ask for the server a receipt for this frame. When a receipt arrived the
	 * {@link StompMessageStateCallback#receiptReceived()} method will be called. This receipt is used to notify the
	 * submitter than his message has been treated.
	 * 
	 * @param topic
	 * @param message
	 * @throws NotConnectedException
	 * @throws SocketException
	 * @see {@link #send(String, String, Map, StompMessageStateCallback)}
	 */
	public void send(String topic, String message, Map<String, String> additionalHeaders,
			StompMessageStateCallback callback) throws NotConnectedException, SocketException {
		clientHandler.send(channel, topic, message, additionalHeaders, callback);
	}

	public void addListener(StompClientListener stompClientListener) {
		clientHandler.addListener(stompClientListener);
	}

	public void removeListener(StompClientListener stompClientListener) {
		clientHandler.removeListener(stompClientListener);
	}

	public long getGuaranteedHeartBeat() {
		return clientHandler.getLocalGuaranteedHeartBeat();
	}

	public long getExpectedHeartBeat() {
		return clientHandler.getLocalExpectedHeartBeat();
	}

	/**
	 * Configure the head-beat system.
	 * 
	 * <p/>
	 * <b>NOTE :</b> This can only be set while the client is not connected yet.
	 * 
	 * @param guaranteedHeartBeat
	 * @param expectedHeartBeat
	 * @throws RuntimeException
	 */
	public void setHeartBeat(long guaranteedHeartBeat, long expectedHeartBeat) throws RuntimeException {
		if (clientHandler.isLoggedIn())
			throw new RuntimeException("You can't change heart-beat parameters while the client is connected to server");
		clientHandler.setHeartBeat(guaranteedHeartBeat, expectedHeartBeat);
	}

}
