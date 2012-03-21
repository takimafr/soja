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
package com.excilys.soja.client.events;

/**
 * This class is used to notify the state of a message sent by a STOMP client.
 * <p/>
 * If the callback is used, the client will automaticaly ask the server for a receipt. When the receipt is received, the
 * method {@link #receiptReceived()} is called. Else, the client application is notified of the error (timeout, error command,
 * etc...) by the {@link #onError(String, String)} method.
 * 
 * @author dvilleneuve
 * 
 */
public interface StompMessageStateCallback {

	/**
	 * This method is called when a message sent by the client has been correctly received and processed by the server.
	 */
	void receiptReceived();

}
