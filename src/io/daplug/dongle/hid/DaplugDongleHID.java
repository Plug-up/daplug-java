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

import java.io.IOException;
import java.util.Vector;

import io.daplug.exception.*;
import io.daplug.dongle.hid.HIDComm;
import io.daplug.dongle.hid.HIDExec;

import com.codeminders.hidapi.HIDDevice;
import com.codeminders.hidapi.HIDDeviceInfo;

public class DaplugDongleHID {

	private HIDDevice device = null;
	private boolean deviceStatus;
	private HIDExec hidexec = null;
	private HIDComm hidComm = null;
	private static DaplugDongleHID _this = null;
	
	static {
		com.codeminders.hidapi.ClassPathLibraryLoader.loadNativeHIDLibrary();
	}
	
	/**
	 * Simple constructor.  The user has to initial all compoment manually.
	 * @author yassir
	 */
	public DaplugDongleHID(){
		_this = this;
		this.hidexec = new HIDExec();
		this.hidComm = new HIDComm();
		this.deviceStatus = true;
	}
	
	/**
	 * DaplugDongleHID constructor with in parameter the hid device path.
	 * this constructor create the hid device with the given path
	 * @param path of device
	 * @author yassir
	 */
	public DaplugDongleHID(String path){
		_this = this;
		this.deviceStatus = true;
		this.hidexec = new HIDExec();
		this.device = this.openDevice(path);
		this.hidComm = new HIDComm(this.device, this.deviceStatus);
		
	
	}
	
	/**
	 * constructor which initialise specialy the HIDComm constructor
	 * @param device HIDDevice 
	 * @param deviceStatus boolean
	 * @author yassir
	 */
	public DaplugDongleHID(HIDDevice device, boolean deviceStatus){
		_this = this;
		this.hidexec = new HIDExec();
		this.device = device;
		this.deviceStatus = deviceStatus;
		this.hidComm = new HIDComm(device, deviceStatus);
	}
	
	/**
	 * List all HID Device on the computer
	 * @return HIDDeviceInfo[] array of all HID Device
	 * @author yassir
	 */
	public static HIDDeviceInfo[] ListAllHidDevice()
	{
		return _this.hidexec.listDevice();
	}
	
	/**
	 * List all devices with a specific vendor_id, and product_id
	 * @param vid vendor_id
	 * @param pid product_id
	 * @return Vector<HIDDeviceInfo> List of all device with specific vid and pid
	 * @author yassir
	 */
	public static Vector<HIDDeviceInfo> ListAllSpecificHidDevice(int vid, int pid)
	{
		return _this.hidexec.listDevice(vid, pid);
	}
	
	/**
	 * List all device on computer with Plugup vendor_id and product_id
	 * @return a List all device
	 * @author yassir
	 */
	public static Vector<HIDDeviceInfo> ListAllDaplug()
	{
		return _this.hidexec.listAllDaplug();
	}
	
	/**
	 * @return the device
	 * @author yassir
	 */
	public HIDDevice getDevice() {
		return this.device;
	}
	
	/**
	 * Open a Daplug device
	 * @return HIDDevice device
	 * @author yassir
	 */
	public HIDDevice openDevice() {
		return this.hidexec.openDevice();
	}
	
	/**
	 * Open a specific device
	 * @param vid vendor_id
	 * @param pid product_id
	 * @return HIDDevice device
	 * @author yassir
	 */
	public HIDDevice openDevice(int vid, int pid) {
		return this.hidexec.openDevice(vid, pid);
	}
	
	/**
	 * Open a device with his path
	 * @param path device path
	 * @return HIDDevice device
	 * @author yassir
	 */
	public HIDDevice openDevice(String path) {
		return this.hidexec.openDevice(path);
	}
	
	/**
	 * close the HIDDevice in parameter
	 * @param device HIDDevice
	 * @author yassir
	 */
	public void close(HIDDevice device) {
		try {
			device.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * close the current HIDDevice
	 * @author yassir
	 */
	public void close() {
		try {
			this.device.close();
			this.deviceStatus = false;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * exchange command with the daplugDongle. It return a String array which element is :
	 * element 0 : the response from daplugDongleHID at element 1 : the status word
	 * @param apdu byte[]
	 * @return String [] result
	 * @throws IOException
	 * @throws PlugupException
	 * @throws PlugupCommunicationException
	 * @throws PlugupInvalidStatusWordException
	 * @author yassir
	 */
	public String[] exchange(byte[]apdu) throws DaplugException,
	DaplugCommunicationException,DaplugStatusWordException, IOException
	{
		return this.hidComm.exchange(apdu);	
	}
	
	/**
	 * exchange command with the daplugDongle. It return a String array which element is :
	 * element 0 : the response from daplugDongleHID at element 1 : the status word
	 * @param apdu String
	 * @return String [] result
	 * @throws IOException
	 * @throws PlugupException
	 * @throws PlugupCommunicationException
	 * @throws PlugupInvalidStatusWordException
	 * @author yassir
	 */
	public String[] exchange(String apdu) throws DaplugException,
	DaplugCommunicationException,DaplugStatusWordException, IOException 
	{
		return this.hidComm.exchange(apdu);	
	}
	
	/**
	 * The RESET function performs a warm reset of the Plug-up dongle. It is freely available (as long as the dongle is not halted by the HALT command).
	 * @return String [] result
	 * @throws IOException
	 * @throws PlugupException
	 * @throws PlugupCommunicationException
	 * @throws PlugupInvalidStatusWordException
	 * @author yassir
	 */
	public String[] backToOrigin() throws DaplugException,
	DaplugCommunicationException,DaplugStatusWordException, IOException 
	{
		String apduOrigin = "D052010000";
		return this.exchange(apduOrigin);
	}
	
	/**
	 * @param device the device to set
	 * @author yassir
	 */
	public void setDevice(HIDDevice device) {
		this.device = device;
	}

	/**
	 * @return the deviceStatus
	 * @author yassir
	 */
	public boolean isDeviceStatus() {
		return deviceStatus;
	}

	/**
	 * @param deviceStatus the deviceStatus to set
	 * @author yassir
	 */
	public void setDeviceStatus(boolean deviceStatus) {
		this.deviceStatus = deviceStatus;
	}
	
	/**
	 * 
	 * @return the hidComm Object
	 */
	public HIDComm getHidComm() {
		return hidComm;
	}
	
	/**
	 * 
	 * @param hidComm the HIDComm object to set
	 * @author yassir
	 */
	public void setHidComm(HIDComm hidComm) {
		this.hidComm = hidComm;
	}
	
}
