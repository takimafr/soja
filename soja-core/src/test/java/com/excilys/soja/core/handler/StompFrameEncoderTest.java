package com.excilys.soja.core.handler;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.util.CharsetUtil;
import org.junit.Test;

import com.excilys.soja.core.model.Frame;

public class StompFrameEncoderTest {

	private StompFrameEncoder frameEncoder = new StompFrameEncoder();

	@Test
	public void testEscapeHeader() throws Exception {
		assertEquals("test\\ntest", StompFrameEncoder.escapeHeader("test\ntest"));
		assertEquals("test\\ctest", StompFrameEncoder.escapeHeader("test:test"));
		assertEquals("test\\\\test", StompFrameEncoder.escapeHeader("test\\test"));

		assertEquals("test\\cline1\\nline2 \\\\b\\\\", StompFrameEncoder.escapeHeader("test:line1\nline2 \\b\\"));
	}

	@Test
	public void testEscapeHeader_null() throws Exception {
		assertNull(StompFrameEncoder.escapeHeader(null));
	}

	@Test
	public void testEncode() throws Exception {
		Frame frame = new Frame();
		frame.setCommand(Frame.COMMAND_SEND);
		frame.setHeaderValue("test-key1", "test-value1");
		frame.setHeaderValue("test:\n\\key2", "test:\n\\value2");
		frame.setBody("body test");

		ChannelBuffer frameBuffer = (ChannelBuffer) frameEncoder.encode(null, null, frame);
		String frameString = frameBuffer.toString(CharsetUtil.UTF_8);

		assertEquals("SEND\ntest-key1:test-value1\ntest\\c\\n\\\\key2:test\\c\\n\\\\value2\n\nbody test\0", frameString);
	}

	@Test
	public void testEncode_frame_null() throws Exception {
		assertNull(frameEncoder.encode(null, null, null));
	}

	@Test
	public void testEncode_header_null() throws Exception {
		Frame frame = new Frame();
		frame.setCommand(Frame.COMMAND_SEND);
		frame.setBody("body test");

		ChannelBuffer frameBuffer = (ChannelBuffer) frameEncoder.encode(null, null, frame);
		String frameString = frameBuffer.toString(CharsetUtil.UTF_8);

		assertEquals("SEND\n\nbody test\0", frameString);
	}

	@Test
	public void testEncode_body_null() throws Exception {
		Frame frame = new Frame();
		frame.setCommand(Frame.COMMAND_SEND);
		frame.setHeaderValue("test-key", "test-value");

		ChannelBuffer frameBuffer = (ChannelBuffer) frameEncoder.encode(null, null, frame);
		String frameString = frameBuffer.toString(CharsetUtil.UTF_8);

		assertEquals("SEND\ntest-key:test-value\n\n\0", frameString);
	}

	@Test
	public void testEncode_header_body_null() throws Exception {
		Frame frame = new Frame();
		frame.setCommand(Frame.COMMAND_SEND);

		ChannelBuffer frameBuffer = (ChannelBuffer) frameEncoder.encode(null, null, frame);
		String frameString = frameBuffer.toString(CharsetUtil.UTF_8);

		assertEquals("SEND\n\n\0", frameString);
	}

}
