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

package io.daplug.dongle.winusb;

import java.util.Vector;

import org.usb4java.*;
import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;
import org.usb4java.DeviceList;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;

import io.daplug.utils.DaplugUtils;

public class DaplugDongleWinusb implements IWinusb{
	private Context context = null;
	private static Vector<Device> allDevices = new Vector<Device>();
	private WinusbComm winusbcomm = null; 
	private Device device = null;
	
	/**
	 * Simple Constructor
	 */
	public DaplugDongleWinusb() {
		initContext();
		this.findAllDevices();
	}
	
	/**
	 * Constructor where the user can give the path to open the Daplug Dongle
	 * @param path Strimg 
	 */
	public DaplugDongleWinusb(String path) {
		//Edited by: s.benamar@plug-up.com 24/03/2015	
		//No need for this
		//initContext();
		//this.findAllDevices();
		//
		this.device = this.openDaplugWinusb(path);
		this.winusbcomm = new WinusbComm(this.device);
	}

	/**
	 * initialize the WinUSb Context. According to usb4java documentation, it
	 * initialise the C++ WinUSB Struct
	 * 
	 */
	private void initContext() {
		this.context = new Context();
		int result = LibUsb.init(context);
		if (result != LibUsb.SUCCESS)
			throw new LibUsbException("Unable to initialize libusb.", result);
	}

	/**
	 * free the Winusb C++ struct According to usb4java documentation
	 * 
	 * @author yassir
	 */
	public void CloseContext() {
		LibUsb.exit(this.context);
	}

	/**
	 * List all devices on the computer.
	 * 
	 * @return Vector<Device>
	 * @author yassir
	 */
	private void findAllDevices() {
		//Edited by: s.benamar@plug-up.com 24/03/2015
		//Initialize the vector each time the method is called
		allDevices.clear();
		//
//		Vector<Device> v_dev = new Vector<Device>();
		DeviceList list = new DeviceList();
		//Edited by: s.benamar@plug-up.com 20/03/2015
		//Context parameter is set to the current context
		int result = LibUsb.getDeviceList(this.context, list);
		//
		if (result < 0)
			throw new LibUsbException("Unable to get device list", result);
		try {
			for (Device device : list) {
				allDevices.add(device);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			//Ensure the allocated device list is freed
			//Edited by: s.benamar@plug-up.com 20/03/2015
			//The unrefDevices parameter is not set (=false)
			LibUsb.freeDeviceList(list, false);
			//
		}
	}

	/**
	 * List all Devices on the Computer with a specific vid and pid value
	 * 
	 * @param vid short
	 * @param pid short
	 * @return Vector<Device> 
 	 * @author yassir
 	 */
	private Vector<Device> findAllDevices(short vid, short pid) {
		Vector<Device> v_dev = new Vector<Device>();
		try {
			// Iterate over all devices and scan for the right one
			for (Device device : allDevices) {
				DeviceDescriptor descriptor = new DeviceDescriptor();
				int result = LibUsb.getDeviceDescriptor(device, descriptor);
				if (result != LibUsb.SUCCESS)
					throw new LibUsbException(
							"Unable to read device descriptor", result);
				if (descriptor.idVendor() == vid
						&& descriptor.idProduct() == pid)
					v_dev.add(device); // return device;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return v_dev;
	}
	
	/**
	 * List all Devices on the Computer with the specific vid and pid value for DaplugDongle
	 * @return Vector<Device>
	 * @author yassir
	 */
	public Vector<Device> findAllDaplugDevices() {
		return findAllDevices(VENDOR_ID, PRODUCT_ID);	
	}
	
	/**
	 * Get All informations and print it to screen form a specific Device
	 * @param device Device
	 * @return String
	 * @author yassir
	 */
	public static String dumpDaplugWinUSB(Device device) {
		DeviceDescriptor des = new DeviceDescriptor();
		LibUsb.getDeviceDescriptor(device, des);
		String res = " vid                    : " + des.idVendor()
				+ "\n pid                    : " + des.idProduct()
				+ "\n Manufacturer           : " + des.iManufacturer()
				+ "\n bcdevice               : " + des.bcdDevice()
				+ "\n bcdUSB                 : " + des.bcdUSB()
				+ "\n serial                 : " + des.iSerialNumber()
				+ "\n bDescriptorType        : " + des.bDescriptorType()
				+ "\n bDeviceClass           : " + des.bDeviceClass()
				+ "\n bDeviceProtocol        : " + des.bDeviceProtocol()
				+ "\n bDeviceSubClass        : " + des.bDeviceSubClass()
				+ "\n bLength                : " + des.bLength()
				+ "\n bMaxPacketSize0        : " + des.bMaxPacketSize0()
				+ "\n bNumConfigurations     : " + des.bNumConfigurations()
				+ "\n getBuffer              : " + des.getBuffer()
				+ "\n getPointer             : " + device.getPointer()
				+ "\n hashCode               : " + des.hashCode();
		return res;
	}
	
	/**
	 * Create List all Device with their path. It's an other way to list devices by their path.
	 * Please Notice that this path is something created specifically for this case.
	 * 
	 * @return Vector<String> List of all device path
	 * @author yassir
	 */
	public Vector<String> getPath() {
		Vector<String> v_res = new Vector<String>(); 
		for(Device device : allDevices) {
			DeviceDescriptor des = new DeviceDescriptor();
			LibUsb.getDeviceDescriptor(device, des);
			int i = 0;
			if (PRODUCT_ID == des.idProduct() && VENDOR_ID == des.idVendor()) {
				StringBuilder sb = new StringBuilder();
				sb.append("Dongle ").append(i).append(",WINUSB").append(",").append(device.getPointer()).append(",").append("Plug-up");
				v_res.addElement(sb.toString());
				i++;
			}
		}
		return v_res;
	}
	
	
	/**
	 * Open DaplugDongle with a specific vendor_id and product_id
	 * @param vid short the product vendor_id
	 * @param pid short the product product_id
	 * @return Vector<String> open Daplug Dongle
	 * @author yassir
	 */
	private Vector<Device> openDaplugWinusb(short vid, short pid) {
		Vector<Device> v_dev = new Vector<Device>();
		for (Device device : allDevices) {
			DeviceDescriptor descriptor = new DeviceDescriptor();
			int result = LibUsb.getDeviceDescriptor(device, descriptor);
			if (result != LibUsb.SUCCESS)
				throw new LibUsbException(
						"Unable to read device descriptor", result);
			if (descriptor.idVendor() == vid
					&& descriptor.idProduct() == pid) {
				v_dev.add(device);
			}
		}
		return v_dev;
	}
	
	/**
	 * Open a Daplug Dongle Winusb Interface with his specific vendor_id and product_id
	 * @return Device device
	 * @author yassir
	 */
	public Vector<Device> openDaplugWinusb() {	
		return this.openDaplugWinusb(VENDOR_ID, PRODUCT_ID);
	}
	
	/**
	 * Open a Specific Daplug Dongle with his path value. Please Notice that this path is a only for use in this case
	 * 
	 * @param path String
	 * @return Device
	 * @author yassir
	 */
	public Device openDaplugWinusb(String path) {
		int i = 0;
		Vector<Device> v_dev = this.openDaplugWinusb();
		for (Device device : v_dev) {
			String pointer = String.valueOf(v_dev.elementAt(i).getPointer());
			if(path.equals(pointer))
				return device;
			i++;
		}
		return null;
	}
	
	/** make an exchange with a daplug dongle winusb. 
	 * return value is a String [] with 2 values :
	 * first the data received if exist
	 * second the status word
	 * @param apdu byte [] apdu to send to the dongle
	 * @return String[2] result
	 * @author yassir
	 */
	public String[] exchange(byte [] apdu) {
		return this.winusbcomm.exchange(apdu);
	}
	

	/** make an exchange with a daplug dongle winusb. 
	 * return value is a String [] with 2 values :
	 * first the data received if exist
	 * second the status word
	 * @param apdu String apdu to send to the dongle
	 * @return String[2] result
	 * @author yassir
	 */
	public String [] exchange(String apdu) {
		return this.winusbcomm.exchange(DaplugUtils.hexStringToByteArray(apdu));
	}
	
}
