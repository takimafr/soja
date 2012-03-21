/**
###############################################################################
# Contributors:
#     Damien VILLENEUVE - initial API and implementation
###############################################################################
 */
package com.excilys.soja.netty;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.Delimiters;

import com.excilys.soja.netty.handler.FrameDecoder;
import com.excilys.soja.netty.handler.FrameEncoder;


/**
 * @author dvilleneuve
 * 
 */
public class StompPipelineFactory implements ChannelPipelineFactory {

	public ChannelPipeline getPipeline() throws Exception {
		// Create a default pipeline implementation.
		ChannelPipeline pipeline = Channels.pipeline();

		// Add the text line codec combination first,
		// TODO: A null delimiter will split a frame with a null character in a body stream...
		pipeline.addLast("framer", new DelimiterBasedFrameDecoder(Integer.MAX_VALUE, Delimiters.nulDelimiter()));
		pipeline.addLast("frameDecoder", new FrameDecoder());
		pipeline.addLast("frameEncoder", new FrameEncoder());

		return pipeline;
	}

}
