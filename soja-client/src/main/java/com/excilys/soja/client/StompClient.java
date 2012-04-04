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

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.excilys.soja.client.events.StompClientListener;
import com.excilys.soja.client.events.StompMessageStateCallback;
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

	private final String hostname;
	private final int port;
	private final ClientBootstrap clientBootstrap;
	private final ClientHandler clientHandler;

	private Channel channel;
	private ChannelFuture channelFuture;

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
	 * Connect to the server as a guest.
	 * <p/>
	 * See {@link StompClient#connect(String, String)}
	 * 
	 * @return true if connected to server, false else
	 */
	public boolean connect() {
		return connect(null, null);
	}

	/**
	 * Connect to the server and return true if the client is physically connected to the server. This doesn't mean the
	 * client is connected accordingly to STOMP protocol : the shakehand phase will start automaticaly just after this
	 * method. Return false if the server is unreachable.
	 * 
	 * @param username
	 * @param password
	 * @return true if connected to server, false else
	 */
	public boolean connect(String username, String password) {
		// Start the connection attempt.
		channelFuture = clientBootstrap.connect(new InetSocketAddress(hostname, port));

		// Wait until the connection attempt succeeds or fails.
		channel = channelFuture.awaitUninterruptibly().getChannel();
		if (!channelFuture.isSuccess()) {
			LOGGER.error("Client failed to connect to {}:{}", new Object[] { hostname, port, channelFuture.getCause() });

			clientBootstrap.releaseExternalResources();
			return false;
		}

		if (channel.isConnected()) {
			LOGGER.debug("Connected to {}:{}. Login with username {}...", new Object[] { hostname, port, username });

			// When connected, send a CONNECT command
			clientHandler.connect(channel, SUPPORTED_STOMP_VERSION, hostname, username, password);
			return true;
		}
		return false;
	}

	/**
	 * Disconnect the STOMP client from the server.
	 */
	public void disconnect() {
		disconnect(null);
	}

	/**
	 * Disconnect the STOMP client from the server.
	 */
	public void disconnect(StompMessageStateCallback callback) {
		LOGGER.debug("disconnecting...");

		clientHandler.sendFrame(channel, new DisconnectFrame(), callback);

		// Wait until all messages are flushed before closing the channel.
		if (channelFuture != null) {
			channelFuture.awaitUninterruptibly();
		}

		if (channel != null && channel.isOpen()) {
			// Close the connection. Make sure the close operation ends because
			// all I/O operations are asynchronous in Netty.
			channel.close().awaitUninterruptibly();
		}

		if (clientBootstrap != null) {
			// Shut down all thread pools to exit.
			clientBootstrap.releaseExternalResources();
		}

		LOGGER.debug("disconnected");
	}

	public Long subscribe(String topic) {
		return subscribe(topic, null, Ack.AUTO);
	}

	public Long subscribe(String topic, StompMessageStateCallback callback, Ack ackMode) {
		return clientHandler.subscribe(channel, topic, callback, ackMode);
	}

	public void unsubscribe(Long subscriptionId) {
		unsubscribe(subscriptionId, null);
	}

	public void unsubscribe(Long subscriptionId, StompMessageStateCallback callback) {
		clientHandler.unsubscribe(channel, subscriptionId, callback);
	}

	public void send(String topic, String message) {
		send(topic, message, null, null);
	}

	public void send(String topic, String message, StompMessageStateCallback callback) {
		send(topic, message, null, callback);
	}

	public void send(String topic, String message, Map<String, String> additionalHeaders,
			StompMessageStateCallback callback) {
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

	public void setHeartBeat(long guaranteedHeartBeat, long expectedHeartBeat) throws RuntimeException {
		if (clientHandler.isLoggedIn())
			throw new RuntimeException("You can't change heart-beat parameters while the client is connected to server");
		clientHandler.setHeartBeat(guaranteedHeartBeat, expectedHeartBeat);
	}

}
