/**
###############################################################################
# Contributors:
#     Damien VILLENEUVE - initial API and implementation
###############################################################################
 */
package com.excilys.stomp.netty.handler;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.LoginException;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.excilys.stomp.authentication.AllowAllAuthentication;
import com.excilys.stomp.authentication.Authentication;
import com.excilys.stomp.model.Frame;
import com.excilys.stomp.netty.ClientRemoteSession;

/**
 * @author dvilleneuve
 * 
 */
public class ServerHandler extends StompHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServerHandler.class);

	private Authentication globalAuthentication;
	private Map<Integer, ClientRemoteSession> clientSessions = new HashMap<Integer, ClientRemoteSession>();

	public ServerHandler() {
		this.globalAuthentication = AllowAllAuthentication.INSTANCE;
	}

	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		super.channelConnected(ctx, e);
		LOGGER.debug("Client {} connected. Starting client session", ctx.getChannel().getRemoteAddress());

		// When a client connect to the server, we allocate to him a ClientSession instance (based on the channel id)
		Integer channelId = e.getChannel().getId();
		if (!clientSessions.containsKey(channelId)) {
			clientSessions.put(channelId, new ClientRemoteSession(e.getChannel(), globalAuthentication));
		}
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent event) throws Exception {
		if (!(event.getMessage() instanceof Frame)) {
			LOGGER.error("Not a frame... {}", event.getMessage());
			return;
		}
		Frame frame = (Frame) event.getMessage();
		LOGGER.debug("Received frame : {}", frame);

		// Retrieve the session for this client
		ClientRemoteSession clientRemoteSession = clientSessions.get(event.getChannel().getId());
		if (clientRemoteSession != null) {
			// CONNECT
			if (frame.isCommand(Frame.COMMAND_CONNECT)) {
				try {
					clientRemoteSession.handleConnect(frame);
				} catch (LoginException e) {
					disconnectClientSession(event.getChannel());
				}
			}
			// DISCONNECT
			else if (frame.isCommand(Frame.COMMAND_DISCONNECT)) {
				clientRemoteSession.handleDisconnect(frame);
				disconnectClientSession(event.getChannel());
			}
			// SUBSCRIBE
			else if (frame.isCommand(Frame.COMMAND_SUBSCRIBE)) {
				clientRemoteSession.handleSubscribe(frame);
			}
			// UNSUBSCRIBE
			else if (frame.isCommand(Frame.COMMAND_UNSUBSCRIBE)) {
				clientRemoteSession.handleUnsubscribe(frame);
			}
			// SEND
			else if (frame.isCommand(Frame.COMMAND_SEND)) {
				clientRemoteSession.handleSend(frame);
			}
		}
	}

	private void disconnectClientSession(Channel channel) {
		channel.close().awaitUninterruptibly(15000);
		clientSessions.remove(channel.getId());
	}

	public Authentication getAuthentication() {
		return globalAuthentication;
	}

	public void setAuthentication(Authentication authentication) {
		synchronized (this.globalAuthentication) {
			this.globalAuthentication = authentication;
		}
	}

}
