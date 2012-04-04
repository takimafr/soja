/**
 * Copyright 2010-2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.excilys.soja.core.handler;

import java.util.Timer;
import java.util.TimerTask;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.excilys.soja.core.model.Frame;
import com.excilys.soja.core.model.Header;
import com.excilys.soja.core.model.frame.HeartBeatFrame;

/**
 * @author dvilleneuve
 * 
 */
public abstract class StompHandler extends SimpleChannelHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(StompHandler.class);

	private long localGuaranteedHeartBeat;
	private long localExpectedHeartBeat;
	private Timer localHeartBeartTimer;

	@Override
	public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		super.channelDisconnected(ctx, e);
		if (localHeartBeartTimer != null) {
			localHeartBeartTimer.cancel();
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		super.exceptionCaught(ctx, e);
		LOGGER.error("Remote end channel is closed", e);
	}

	/**
	 * Handle HEARTBEAT command
	 * 
	 * @param frame
	 */
	public void handleHeartBeat(final Channel channel, Frame frame) {
		// TODO: manage heart-beat
	}

	/**
	 * Send a STOMP frame to the remote
	 * 
	 * @param frame
	 */
	public void sendFrame(Channel channel, Frame frame) {
		LOGGER.debug("Sending : {}", frame);
		channel.write(frame);
	}

	/**
	 * Start a scheduler for the heart-beating system.
	 * 
	 * @param channel
	 * @param remoteExpectedHeartBeat
	 */
	public boolean startLocalHeartBeat(final Channel channel, final Frame frame) {
		String heartBeatString = frame.getHeaderValue(Header.HEADER_HEART_BEAT);
		if (heartBeatString != null) {
			String[] heartBeat = heartBeatString.split(",");
			if (heartBeat.length == 2) {
				// long remoteGuaranteedHeartBeat = Long.parseLong(heartBeat[0]);
				long remoteExpectedHeartBeat = Long.parseLong(heartBeat[1]);

				// Check it the server is expecting a hear-beating and if the client can do it
				if (localGuaranteedHeartBeat != 0 && remoteExpectedHeartBeat != 0) {
					long heartBeatInterval = Math.max(localGuaranteedHeartBeat, remoteExpectedHeartBeat);

					// Launch a scheduler
					localHeartBeartTimer = new Timer("Heart-beating");
					localHeartBeartTimer.scheduleAtFixedRate(new TimerTask() {
						@Override
						public void run() {
							sendFrame(channel, new HeartBeatFrame());
						}
					}, 0, heartBeatInterval);

					return true;
				}
			}
		}
		return false;
	}

	public long getLocalGuaranteedHeartBeat() {
		return localGuaranteedHeartBeat;
	}

	public long getLocalExpectedHeartBeat() {
		return localExpectedHeartBeat;
	}

	/**
	 * Set the heart-bearting parameters on client-side
	 * 
	 * @param localGuaranteedHeartBeat
	 *            smallest number of milliseconds between heart-beats that this local part can guarantee
	 * @param localExpectedHeartBeat
	 *            the desired number of milliseconds between remote's heart-beats
	 * @throws IllegalArgumentException
	 *             if guaranteedHeartBeat or expectedHearBeat are minus to 0
	 */
	public void setHeartBeat(long localGuaranteedHeartBeat, long localExpectedHeartBeat)
			throws IllegalArgumentException {
		if (localGuaranteedHeartBeat < 0)
			throw new IllegalArgumentException("Minimum heart-beat guaranteed have to be a positive number");
		if (localExpectedHeartBeat < 0)
			throw new IllegalArgumentException("Desired interval between heart-beat have to be a positive number");

		this.localGuaranteedHeartBeat = localGuaranteedHeartBeat;
		this.localExpectedHeartBeat = localExpectedHeartBeat;
	}

}
