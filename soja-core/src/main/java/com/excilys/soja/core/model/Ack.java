/**
###############################################################################
# Contributors:
#     Damien VILLENEUVE - initial API and implementation
###############################################################################
 */
package com.excilys.soja.core.model;


/**
 * This enum is used when a client subscribe to a topic.
 * <p/>
 * The default value is {@link #AUTO} which mean no ACK frames will be send back to server.
 * 
 * @author dvilleneuve
 * 
 */
public enum Ack {

	/**
	 * The client <b>doesn't need to send</b> the server ACK frames for the messages it receives. The server will assume
	 * the client has received the message as soon as it sends it to the the client. This acknowledgment mode can cause
	 * messages being transmitted to the client to get dropped.
	 */
	AUTO("auto"),

	/**
	 * The client <b>must send</b> the server ACK frames for the messages it processes.
	 * <p/>
	 * The ACK frames sent by the client will <b>be treated</b> as a cumulative ACK. This means the ACK operates on the
	 * message specified in the ACK frame and all messages sent to the subscription before the ACK-ed message.
	 */
	CLIENT("client"),

	/**
	 * The client <b>must send</b> the server ACK frames for the messages it processes.
	 * <p/>
	 * The ACK frames sent by the client will <b>not be treated</b> as a cumulative ACK. This means that an ACK or NACK
	 * for a subsequent message MUST NOT cause a previous message to get acknowledged.
	 */
	CLIENT_INDIVIDUAL("client-individual");

	private String value;

	private Ack(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}

	public static Ack parseAck(String value) {
		if (CLIENT.toString().equalsIgnoreCase(value)) {
			return CLIENT;
		} else if (CLIENT_INDIVIDUAL.toString().equalsIgnoreCase(value)) {
			return CLIENT_INDIVIDUAL;
		}
		return AUTO;
	}
}
