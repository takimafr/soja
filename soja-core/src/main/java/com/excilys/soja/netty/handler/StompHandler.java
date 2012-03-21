/**
###############################################################################
# Contributors:
#     Damien VILLENEUVE - initial API and implementation
###############################################################################
 */
package com.excilys.soja.netty.handler;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.excilys.soja.model.Frame;


/**
 * @author dvilleneuve
 * 
 */
public abstract class StompHandler extends SimpleChannelHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(ClientHandler.class);

	/**
	 * Send a STOMP frame to the remote
	 * 
	 * @param frame
	 */
	public void sendFrame(Channel channel, Frame frame) {
		LOGGER.debug("Sending : {}", frame);
		channel.write(frame);
	}

}
