package com.excilys.soja.core.handler;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;

import com.excilys.soja.core.model.Frame;

public class StompFrameDecoderTest {

	private StompFrameDecoder frameDecoder = new StompFrameDecoder();

	private Object decode(String value) throws Exception {
		ChannelBuffer channelBuffer = ChannelBuffers.wrappedBuffer(value.getBytes());
		return frameDecoder.decode(null, null, channelBuffer);
	}

	@Test
	public void testDecode() throws Exception {
		Frame expectedFrame = new Frame();
		expectedFrame.setCommand(Frame.COMMAND_SEND);
		expectedFrame.setHeaderValue("test-key", "test-value");
		expectedFrame.setBody("body test");

		assertEquals(expectedFrame, decode("SEND\ntest-key:test-value\n\nbody test\0"));
	}

	@Test
	public void testDecode_empty() throws Exception {
		assertNull(decode(""));
	}

	@Test
	public void testDecode_header_null() throws Exception {
		Frame expectedFrame = new Frame();
		expectedFrame.setCommand(Frame.COMMAND_SEND);
		expectedFrame.setBody("body test");

		assertEquals(expectedFrame, decode("SEND\n\nbody test\0"));
	}

	@Test
	public void testDecode_body_null() throws Exception {
		Frame expectedFrame = new Frame();
		expectedFrame.setCommand(Frame.COMMAND_SEND);
		expectedFrame.setHeaderValue("test-key", "test-value");

		assertEquals(expectedFrame, decode("SEND\ntest-key:test-value\n\n\0"));
	}

	@Test
	public void testDecode_header_body_null() throws Exception {
		Frame expectedFrame = new Frame();
		expectedFrame.setCommand(Frame.COMMAND_SEND);

		assertEquals(expectedFrame, decode("SEND\n\n\0"));
	}

	@Test
	public void testUnescapeHeaderValue() throws Exception {
		assertEquals("test\ntest", StompFrameDecoder.unescapeHeaderValue("test\\ntest"));
		assertEquals("test:test", StompFrameDecoder.unescapeHeaderValue("test\\ctest"));
		assertEquals("test\\test", StompFrameDecoder.unescapeHeaderValue("test\\\\test"));

		assertEquals("test:line1\nline2 \\b\\", StompFrameDecoder.unescapeHeaderValue("test\\cline1\\nline2 \\\\b\\\\"));
	}

	@Test
	public void testUnescapeHeaderValue_null() throws Exception {
		assertNull(StompFrameDecoder.unescapeHeaderValue(null));
	}

}
