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

import java.util.Vector;
import com.codeminders.hidapi.HIDDeviceInfo;
import io.daplug.dongle.hid.HIDExec;
import io.daplug.dongle.winusb.DaplugDongleWinusb;

public class DaplugEnumerator{

	/**
	 * List all HID device
	 * String format in Vector :
	 * HID, vid, pid, path, interface_number, manufactured
	 * @return Vector<String> list of all HID Device
	 * @author yassir
	 */
	private static Vector<String> listDaplugHIDDevice()
	{
		Vector<String> hid = new Vector<String>();
		Vector <HIDDeviceInfo> listHID = new HIDExec().listAllDaplug();
		int i = 0;
		 for(HIDDeviceInfo dev : listHID) {
			 if(dev.getInterface_number() != 0) {
			 StringBuilder sb = new StringBuilder();
			 sb.append("Dongle "+i + ",").append("HID").append(",").
			 	append(dev.getPath()).append(",").
			 	append(dev.getManufacturer_string());
			 hid.add(sb.toString());
			}
		 }
		 return hid;
	}
	
	/**
	 * List all WINUSB device
	 * format String :  WINSUB, vid, pid, path, interface_number, manufactured
	 * @return Vector<String> path of all DaplugDevice in Winusb mode
	 * @author yassir
	 */
	private static Vector<String> listDaplugWinusbDevice() {
		return new DaplugDongleWinusb().getPath();
	}
	
	/**
	 * list all daplug device in HID and WINUSB
	 * format of String : type (HID/WINUSB), vid, pid, path, interface_number, manufactured
	 * all information are separated by a coma (,)
	 * @return Vector<String> list all DaplugDevice 
	 * @author yassir
	 */
	public static Vector<String> listDaplugDongles() {
		System.out.println("List all Daplug Dongle (HID/WINUSB)");
		Vector <String> hid = listDaplugHIDDevice();
		Vector <String> winusb = listDaplugWinusbDevice();
		for(String value : hid)
			System.out.println(value);
		for(String value : winusb)
			System.out.println(value);
		//add ath the end of HID vector list, all data contained by winusb vector
		hid.addAll(winusb);
		return hid;
	}
	
	
	

}

