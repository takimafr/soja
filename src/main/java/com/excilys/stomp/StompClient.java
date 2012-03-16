/**
###############################################################################
# Contributors:
#     Damien VILLENEUVE - initial API and implementation
###############################################################################
 */
package com.excilys.stomp;

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

import com.excilys.stomp.events.StompClientListener;
import com.excilys.stomp.model.Frame;
import com.excilys.stomp.model.frame.ConnectFrame;
import com.excilys.stomp.model.frame.DisconnectFrame;
import com.excilys.stomp.model.frame.SendFrame;
import com.excilys.stomp.model.frame.SubscribeFrame;
import com.excilys.stomp.model.frame.UnsubscribeFrame;
import com.excilys.stomp.netty.StompPipelineFactory;
import com.excilys.stomp.netty.handler.ClientHandler;

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
			channel.write(new ConnectFrame(SUPPORTED_STOMP_VERSION, hostname, username, password));
			return true;
		}
		return false;
	}

	/**
	 * Disconnect the STOMP client from the server.
	 */
	public void disconnect() {
		LOGGER.debug("disconnecting...");

		clientHandler.sendFrame(new DisconnectFrame());

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

	public boolean subscribe(String topic, boolean waitForReceipt) {
		Frame frame = new SubscribeFrame(topic);
		clientHandler.sendFrame(frame);
		return false;
	}

	public boolean unsubscribe(String topic, boolean waitForReceipt) {
		Frame frame = new UnsubscribeFrame(topic);
		clientHandler.sendFrame(frame);
		return false;
	}

	public boolean send(String topic, String message, boolean waitForReceipt) {
		return send(topic, message, null, waitForReceipt);
	}

	public boolean send(String topic, String message, Map<String, String> additionalHeaders, boolean waitForReceipt) {
		Frame frame = new SendFrame(topic, message);
		if (additionalHeaders != null) {
			frame.getHeader().putAll(additionalHeaders);
		}
		clientHandler.sendFrame(frame);
		return false;
	}

	public void addListener(StompClientListener stompClientListener) {
		clientHandler.addListener(stompClientListener);
	}

	public void removeListener(StompClientListener stompClientListener) {
		clientHandler.removeListener(stompClientListener);
	}

}
