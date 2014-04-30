/*
* Copyright Plug-up International SAS (c)
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*
* Authors:
*   Saada BENAMAR <s.benamar@plug-up.com>
*   Yassir Houssen ABDULLAH <a.yassirhoussen@plug-up.com>
*/

package io.daplug.dongle;

import java.io.IOException;

import io.daplug.dongle.hid.DaplugDongleHID;
import io.daplug.dongle.winusb.DaplugDongleWinusb;
import io.daplug.exception.DaplugCommunicationException;
import io.daplug.exception.DaplugException;
import io.daplug.exception.DaplugStatusWordException;
import io.daplug.utils.DaplugUtils;

public class DaplugDongle {

	private String type = null;
	private String path = null;

	private DaplugDongleHID daplugHID = null;
	private DaplugDongleWinusb daplugWINUSB = null;

	/**
	 * This constructor create the specific DaplugDongle (HID/WINUSB) and open it.
	 * The kind of interface used is known by the path given in parameter.
	 * @param value String
	 * @author yassir
	 */
	public DaplugDongle(String value) {
		// if String value is null
		if(value.equals(null))
			throw new NullPointerException("Path does not exist");
			
		String[] res = DaplugUtils.splitString(value, ",");
		this.type = res[1];
		this.path = res[2];
		// initialise daplugHID
		try {
			WhichOneToselect(this.type);
		} catch (DaplugCommunicationException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * This methode initialize one of the two interface(HID/WINUSB) according to
	 * the path the user given
	 * @param type String 
	 * @throws DaplugCommunicationException
	 * @author yassir
	 */
	private void WhichOneToselect(String type) throws DaplugCommunicationException {
		if (this.type.equals("HID"))
				this.daplugHID = new DaplugDongleHID(this.path);
		else
			this.daplugWINUSB = new DaplugDongleWinusb(this.path);
	}
	
	 
	/**
	 * High-Level method to process the exchange with the DaplugDongle whatever is his interface(HID/WINSUB)
	 * 
	 * @param  apdu String
	 * @return result String []
	 * @throws DaplugCommunicationException
	 * @throws DaplugStatusWordException
	 * @throws DaplugException
	 * @throws IOException
	 * @author yassir
	 */
	public String[] exchange(String apdu) throws DaplugCommunicationException,
			DaplugStatusWordException, DaplugException, IOException {
		if (this.type.equals("HID"))
			return this.daplugHID.exchange(apdu);
		else
			return this.daplugWINUSB.exchange(apdu);
	}

	/**
	 * High-Level method to process the exchange with the DaplugDongle whatever is his interface(HID/WINSUB)
	 * 
	 * @param  apdu byte [] 
	 * @return result String []
	 * @throws DaplugCommunicationException
	 * @throws DaplugStatusWordException
	 * @throws DaplugException
	 * @throws IOException
	 * @author yassir
	 */
	public String[] exchange(byte[] apdu) throws DaplugCommunicationException,
	DaplugStatusWordException, DaplugException, IOException {
		if (this.type.equals("HID"))
			return this.daplugHID.exchange(apdu);
		else
			return this.daplugWINUSB.exchange(apdu);
	}
	
	/**
	 * close current device according to the previous path given 
	 * @author yassir
	 */
	public void close() {
		if (this.type.equals("HID"))
			 this.daplugHID.close();
		else
			this.daplugWINUSB.CloseContext();
	}
}
