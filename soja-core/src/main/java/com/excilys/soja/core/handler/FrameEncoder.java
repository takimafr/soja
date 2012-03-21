/**
###############################################################################
# Contributors:
#     Damien VILLENEUVE - initial API and implementation
###############################################################################
 */
package com.excilys.soja.core.handler;

import java.nio.charset.Charset;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

import com.excilys.soja.core.model.Frame;
import com.excilys.soja.core.utils.FrameSerializer;


/**
 * @author dvilleneuve
 * 
 */
public class FrameEncoder extends OneToOneEncoder {

	@Override
	protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
		if (!(msg instanceof Frame)) {
			return msg;
		}
		String frameString = FrameSerializer.serializeFrame((Frame) msg);
		return ChannelBuffers.copiedBuffer((String) frameString, Charset.forName("UTF-8"));
	}
}
