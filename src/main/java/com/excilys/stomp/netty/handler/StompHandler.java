/**
###############################################################################
# Contributors:
#     Damien VILLENEUVE - initial API and implementation
###############################################################################
 */
package com.excilys.stomp.netty.handler;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.excilys.stomp.model.Frame;


/**
 * @author dvilleneuve
 * 
 */
public abstract class StompHandler extends SimpleChannelHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(ClientHandler.class);
	private Channel channel;

	/**
	 * Send a STOMP frame to the remote
	 * 
	 * @param frame
	 */
	public void sendFrame(Frame frame) {
		LOGGER.debug("Sending : {}", frame);
		channel.write(frame);
	}

	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		this.channel = ctx.getChannel();
		super.channelConnected(ctx, e);
	}

}
