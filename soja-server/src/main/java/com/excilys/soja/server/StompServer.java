/**
###############################################################################
# Contributors:
#     Damien VILLENEUVE - initial API and implementation
###############################################################################
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
	private ServerHandler serverHandler;

	public StompServer(int port, Authentication authentication) {
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
	 * Start the server and listen to new connection requests.
	 */
	public boolean start() {
		acceptorChannel = serverBootstrap.bind(new InetSocketAddress(port));
		if (acceptorChannel.isBound()) {
			LOGGER.debug("Server started on port {}. Start listening...", port);
			return true;
		} else {
			LOGGER.debug("Server failed to start on port {}", port);
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

}
