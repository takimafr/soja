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
import com.excilys.soja.server.handler.ServerHandler;

/**
 * @author dvilleneuve
 * 
 */
public class StompServer {

	public static final String STOMP_VERSION = "1.1";
	public static final String SERVER_VERSION = "1.0";
	public static final String SERVER_NAME = "StompServer";

	private static final Logger LOGGER = LoggerFactory.getLogger(StompServer.class);

	private final int port;
	private final ServerBootstrap serverBootstrap;
	private Channel acceptorChannel;

	public StompServer(int port, final Authentication authentication) {
		this.port = port;
		this.serverBootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(Executors.newCachedThreadPool(),
				Executors.newCachedThreadPool()));

		this.serverBootstrap.setPipelineFactory(new StompPipelineFactory() {
			@Override
			public ChannelPipeline getPipeline() throws Exception {
				ChannelPipeline pipeline = super.getPipeline();
				pipeline.addLast("handler", new ServerHandler(authentication));
				return pipeline;
			}
		});
		this.serverBootstrap.setOption("tcpNoDelay", true);
		this.serverBootstrap.setOption("keepAlive", true);
	}

	/**
	 * Start the server and listen to new connection requests.
	 */
	public boolean start() {
		acceptorChannel = serverBootstrap.bind(new InetSocketAddress(port));
		if (acceptorChannel.isBound()) {
			LOGGER.debug("Server started and bound on {}. Start listening...", acceptorChannel.getLocalAddress());
			return true;
		} else {
			LOGGER.debug("Server failed to start on {}", acceptorChannel.getLocalAddress());
			return false;
		}
	}

	/**
	 * Stop the server
	 */
	public void stop() {
		LOGGER.debug("Stopping server...");

		if (acceptorChannel != null && acceptorChannel.isOpen()) {
			// Close the connection. Make sure the close operation ends because
			// all I/O operations are asynchronous in Netty.
			acceptorChannel.close().awaitUninterruptibly();
		}

		if (serverBootstrap != null) {
			// Shut down all thread pools to exit.
			serverBootstrap.releaseExternalResources();
		}

		LOGGER.debug("Server stopped");
	}

	public long getLocalGuaranteedHeartBeat() {
		return ServerHandler.getLocalGuaranteedHeartBeat();
	}

	public long getLocalExpectedHeartBeat() {
		return ServerHandler.getLocalExpectedHeartBeat();
	}

	public void setHeartBeat(long guaranteedHeartBeat, long expectedHeartBeat) {
		ServerHandler.setHeartBeat(guaranteedHeartBeat, expectedHeartBeat);
	}

}
