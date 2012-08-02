package com.excilys.soja.core.handler;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;

import com.excilys.soja.core.model.Frame;

public class StompFrameDecoderTest {

	private StompFrameDecoder frameDecoder = new StompFrameDecoder();

	@Test
	public void testUnescapeHeader() throws Exception {
		assertEquals("test\ntest", StompFrameDecoder.unescapeHeader("test\\ntest"));
		assertEquals("test:test", StompFrameDecoder.unescapeHeader("test\\ctest"));
		assertEquals("test\\test", StompFrameDecoder.unescapeHeader("test\\\\test"));

		assertEquals("test:line1\nline2 \\b\\", StompFrameDecoder.unescapeHeader("test\\cline1\\nline2 \\\\b\\\\"));
	}

	@Test
	public void testUnescapeHeader_null() throws Exception {
		assertNull(StompFrameDecoder.unescapeHeader(null));
	}

	private Object decode(String value) throws Exception {
		ChannelBuffer channelBuffer = ChannelBuffers.wrappedBuffer(value.getBytes());
		return frameDecoder.decode(null, null, channelBuffer, null);
	}

	@Test
	public void testDecode() throws Exception {
		Frame expectedFrame = new Frame();
		expectedFrame.setCommand(Frame.COMMAND_SEND);
		expectedFrame.setHeaderValue("test-key1", "test-value1");
		expectedFrame.setHeaderValue("test:\n\\key2", "test:\n\\value2");
		expectedFrame.setBody("body test");

		assertEquals(expectedFrame, decode("SEND\ntest-key1:test-value1\ntest\\c\\n\\\\key2:test\\c\\n\\\\value2\n\nbody test\0"));
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
	public void testDecode_two_time() throws Exception {
		// TODO: Handle message arriving in two times (ex: 1: SEND\n, 2: test-key:....)
	}

}
