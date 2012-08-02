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
package com.excilys.soja.server;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.excilys.soja.core.factory.StompPipelineFactory;
import com.excilys.soja.server.authentication.Authentication;
import com.excilys.soja.server.events.StompServerListener;
import com.excilys.soja.server.handler.ServerHandler;

/**
 * @author dvilleneuve
 * 
 */
public class StompServer {

	public static final String STOMP_VERSION = "1.1";
	public static final String SERVER_VERSION = "1.0";
	public static final String SERVER_NAME = "Soja";
	public static final String SERVER_HEADER_VALUE = SERVER_NAME + "/" + SERVER_VERSION;

	private static final Logger LOGGER = LoggerFactory.getLogger(StompServer.class);

	private final String hostname;
	private final int port;
	private final ServerBootstrap serverBootstrap;
	private final ServerHandler serverHandler;
	private Channel acceptorChannel;

	public StompServer(String hostname, int port, final Authentication authentication) {
		this.hostname = hostname;
		this.port = port;
		this.serverBootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(Executors.newCachedThreadPool(),
				Executors.newCachedThreadPool()));
		this.serverHandler = new ServerHandler(authentication);

		this.serverBootstrap.setPipelineFactory(new StompPipelineFactory() {
			@Override
			public ChannelPipeline getPipeline() throws Exception {
				ChannelPipeline pipeline = super.getPipeline();
				pipeline.addLast("handler", serverHandler);
				return pipeline;
			}
		});
		this.serverBootstrap.setOption("tcpNoDelay", true);
		this.serverBootstrap.setOption("keepAlive", true);
	}

	/**
	 * Start the server and listen to new client connection requests.
	 */
	public boolean start() {
		acceptorChannel = serverBootstrap.bind(new InetSocketAddress(hostname, port));
		if (acceptorChannel.isBound()) {
			LOGGER.debug("Server started and bound on {}. Start listening...", acceptorChannel.getLocalAddress());
			return true;
		} else {
			LOGGER.debug("Server failed to start on {}", acceptorChannel.getLocalAddress());
			return false;
		}
	}

	/**
	 * Disconnect all clients and stop the server
	 */
	public void stop() {
		LOGGER.debug("Stopping server...");

		// Stop the acceptord channel
		if (acceptorChannel != null && acceptorChannel.isOpen()) {
			// Close the connection. Make sure the close operation ends because
			// all I/O operations are asynchronous in Netty.
			acceptorChannel.close().awaitUninterruptibly();
		}

		// Disconnect all clients
		serverHandler.disconnectAllClients();

		if (serverBootstrap != null) {
			// Shut down all thread pools to exit.
			serverBootstrap.releaseExternalResources();
		}

		LOGGER.debug("Server stopped");
	}

	public void addListener(StompServerListener stompServerListener) {
		serverHandler.addListener(stompServerListener);
	}

	public void removeListener(StompServerListener stompServerListener) {
		serverHandler.removeListener(stompServerListener);
	}

	public long getLocalGuaranteedHeartBeat() {
		return serverHandler.getLocalGuaranteedHeartBeat();
	}

	public long getLocalExpectedHeartBeat() {
		return serverHandler.getLocalExpectedHeartBeat();
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
	public void setHeartBeat(long guaranteedHeartBeat, long expectedHeartBeat) {
		serverHandler.setHeartBeat(guaranteedHeartBeat, expectedHeartBeat);
	}

}
