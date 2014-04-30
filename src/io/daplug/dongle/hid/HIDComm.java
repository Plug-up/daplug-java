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
package io.daplug.dongle.hid;

import io.daplug.exception.*;
import java.io.IOException;

import com.codeminders.hidapi.HIDDevice;

import io.daplug.utils.DaplugUtils;

public class HIDComm implements IHIDComm {

	private HIDDevice device = null;
	private boolean deviceStatus;

	static {
		com.codeminders.hidapi.ClassPathLibraryLoader.loadNativeHIDLibrary();
	}

	public HIDComm() {

	}

	public HIDComm(HIDDevice hid, boolean deviceStatus) {
		this.device = hid;
		this.deviceStatus = deviceStatus;
	}

	/**
	 * send data to specific HIDDevice, and get back his response.
	 * 
	 * @param apdu
	 *            byte[] apdu to send to daplug card
	 * @return result byte []
	 * @throws IOException
	 * @throws PlugupException
	 * @throws DaplugCommunicationException
	 * @throws DaplugStatusWordException
	 * @author yassir
	 */

	private byte[] p_exchange(byte[] apdu) throws DaplugException,
			DaplugCommunicationException, DaplugStatusWordException,
			IOException {
		byte[] w_block = new byte[HID_BLOCK_SIZE + 1];
		byte[] r_block = new byte[HID_BLOCK_SIZE + 1];
		byte[] response = null;
		if (this.deviceStatus == false)
			throw new DaplugException(
					"exchangeApdu(): device Status is closed , please open it again");
		else {
			try {
				int offset = 0;
				int blockSize = 0;
				for (; offset != apdu.length; offset += blockSize) {
					blockSize = ((offset + HID_BLOCK_SIZE) < apdu.length ? HID_BLOCK_SIZE
							: apdu.length - offset);
					System.arraycopy(apdu, offset, w_block, 1, blockSize);
					device.write(w_block);
				}
				// read the result from device and put it in the r_block
				int size = device.readTimeout(r_block, DEFAULT_TIMEOUT);

				if (size < 0) {
					throw new DaplugCommunicationException(
							"exchangeApdu(): Read failure !");
				}
				// response without data
				if (r_block[0] != STATUSWORD_DATA) {
					response = new byte[2];
					// copy r_block content in response
					// the r_block[0] = status word
					// the r_block[1] = response length
					System.arraycopy(r_block, 0, response, 0, 2);
				} else {
					// we get a response with data
					// FROM ubunity team
					int responseSize = (r_block[1] & 0xff);
					if (responseSize == 0) {
						responseSize = 0x100; // T=0 compliance
					}
					responseSize += 2; // include the Status Word
					response = new byte[responseSize];
					offset = 0;
					blockSize = 0;
					for (; offset != responseSize; offset += blockSize) {
						int startOffset = (offset != 0) ? 0 : 2;
						blockSize = ((offset + HID_BLOCK_SIZE - startOffset) < responseSize ? (HID_BLOCK_SIZE - startOffset)
								: responseSize - offset);
						System.arraycopy(r_block, startOffset, response,
								offset, blockSize);
						if (offset == responseSize) {
							break;
						}
						size = device.readTimeout(r_block, DEFAULT_TIMEOUT);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return response;
	}

	/**
	 * send data to specific HIDDevice, and get back his response.
	 * 
	 * @param hexaApdu
	 *            String apdu in hexadecimal format
	 * @return byte[] result
	 * @throws IOException
	 * @throws PlugupException
	 * @throws DaplugCommunicationException
	 * @throws DaplugStatusWordException
	 * @author yassir
	 */
	private byte[] p_exchange(String hexaApdu) throws DaplugException,
			DaplugCommunicationException, DaplugStatusWordException,
			IOException {
		
		byte[] apdu = DaplugUtils.hexStringToByteArray(hexaApdu);
		return this.p_exchange(apdu);
	}

	/**
	 * exchange command with the daplugDongle. It return a String array with at
	 * element 0 : the response from daplugDongle at element 1 : the status word
	 * 
	 * @param apdu
	 *            byte []
	 * @return  result String[]
	 * @throws IOException
	 * @throws PlugupException
	 * @throws DaplugCommunicationException
	 * @throws DaplugStatusWordException
	 * @author yassir
	 */
	public String[] exchange(byte[] apdu) throws DaplugException,
			DaplugCommunicationException, DaplugStatusWordException,
			IOException {
		byte[] tempo = this.p_exchange(apdu);
		String apdu_result = DaplugUtils.bytesToHex(tempo).toString();
		if (apdu_result.length() > 4) { // the result is compose of data + sw
			String[] result = new String[2];
			String status_word = apdu_result.substring(
					apdu_result.length() - 4, apdu_result.length());
			result[0] = apdu_result.substring(0, apdu_result.length() - 4);
			result[1] = status_word;
			return result;
		} else {
			String[] result = new String[2];
			result[0] = "";
			result[1] = apdu_result;
			return result;
		}
	}

	/**
	 * exchange command with the daplugDongle. It return a String array with at
	 * element 0 : the response from daplugDongle at element 1 : the status word
	 * 
	 * @param hexaApdu String
	 * @return result String[]
	 * @throws IOException
	 * @throws PlugupException
	 * @throws DaplugCommunicationException
	 * @throws DaplugStatusWordException
	 * @author yassir
	 */
	@Override
	public String[] exchange(String hexaApdu) throws DaplugException,
			DaplugCommunicationException, DaplugStatusWordException,
			IOException {
		byte[] tempo = this.p_exchange(hexaApdu);
		String apdu_result = DaplugUtils.bytesToHex(tempo).toString();
		if (apdu_result.length() > 4) { // the result is compose of data + sw
			String[] result = new String[2];
			String status_word = apdu_result.substring(
					apdu_result.length() - 4, apdu_result.length());
			result[0] = apdu_result.substring(0, apdu_result.length() - 4);
			result[1] = status_word;
			return result;
		} else {
			String[] result = new String[2];
			result[0] = "";
			result[1] = apdu_result;
			return result;
		}
	}

	/**
	 * return HIDDevice used in the class
	 * @return device HIDDevice 
	 * @author yassir
	 */
	public HIDDevice getDevice() {
		return device;
	}
	
	/**
	 * initialize current HIDDevice device with an other one
	 * @param  device HIDDevice
	 * @author yassir
	 */
	public void setDevice(HIDDevice device) {
		this.device = device;
	}
	
	/**
	 * get HIDDevice Status is open or closed
	 * @return deviceStatus boolean
	 * @author yassir
	 */
	public boolean isDeviceStatus() {
		return deviceStatus;
	}

	/**
	 * set HiDDevice Status to open or closed
	 * @param  deviceStatus boolean
	 * @author yassir
	 */
	public void setDeviceStatus(boolean deviceStatus) {
		this.deviceStatus = deviceStatus;
	}

}
