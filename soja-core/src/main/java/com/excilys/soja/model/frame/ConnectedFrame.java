/**
###############################################################################
# Contributors:
#     Damien VILLENEUVE - initial API and implementation
###############################################################################
 */
package com.excilys.soja.model.frame;

import com.excilys.soja.model.Frame;
import com.excilys.soja.model.Header;

/**
 * @author dvilleneuve
 * 
 */
public class ConnectedFrame extends Frame {

	public ConnectedFrame(String version) {
		super(Frame.COMMAND_CONNECTED, new Header().set(Header.HEADER_VERSION, version), null);
	}

	public ConnectedFrame setSession(String session) {
		getHeader().set(Header.HEADER_SESSION, session);
		return this;
	}

	public ConnectedFrame setServerName(String serverName) {
		getHeader().set(Header.HEADER_SERVER, serverName);
		return this;
	}

}
