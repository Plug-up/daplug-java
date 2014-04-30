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
import com.codeminders.hidapi.HIDDevice;
import com.codeminders.hidapi.HIDDeviceInfo;
import com.codeminders.hidapi.HIDManager;

public class HIDExec implements IHIDExec{
	
	private HIDManager manager = null;
	@SuppressWarnings("unused")
	private boolean status;
	
	static {
		com.codeminders.hidapi.ClassPathLibraryLoader.loadNativeHIDLibrary();
	}

	public HIDExec()
	{
		initManager();
	}
	
	/**
	 * init HIDDevice Manager
	 * @author yassir
	 */
	private void initManager(){
		try {
			this.manager = HIDManager.getInstance();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * set HiDDevice Status to open or closed
	 * @param  deviceStatus boolean
	 * @author yassir
	 */
	public void updateStatusDevice(boolean deviceStatus) {
		this.status = deviceStatus;
	}
	
	/**
	 * Open The device with hid Plug-up vid and pid
	 * @return  daplug HIDDevice
	 * @author yassir
	 */
	public HIDDevice openDevice() {
		HIDDevice device = null;
		try {
			device = this.manager.openById(PRODUCT_ID,VENDOR_ID, null);
			this.updateStatusDevice(true);
		}catch (IOException e){
			System.out.println("OpenDevice() methode failed due to an IOException :\n" + e.getMessage());
		}
		return device;
	} 
	
	/**
	 * open the HIDDevice with specific vis and pid value
	 * @param  vid int vendor_id
	 * @param  pid int product_id
	 * @return HIDDevice
	 * @author yassir
	 */
	public HIDDevice openDevice(int vid, int pid) {
		HIDDevice device = null;
		try {
			device = this.manager.openById(vid, pid, null);
			this.updateStatusDevice(true);
		}catch (IOException e){
			System.out.println("OpenDevice(vid,pid) methode failed due to an IOException :\n" + e.getMessage());
		}
		return device;
	} 
	
	/**
	 * Open Device from his path
	 * @param path  String device path
	 * @return HIDDevice
	 * @author yassir
	 */
	public HIDDevice openDevice(String path) {
		HIDDevice device = null;
		try{
			device = this.manager.openByPath(path);
			this.updateStatusDevice(true);
		}catch (IOException e) {
			System.out.println("openDevice(path) methode failed due to an IOException :\n" + e.getMessage());
		}
		return device;
	}
	
	/**
	 * close all devices whatever his pid and vid
	 * @author yassir
	 */
	public void closeAllDevice() {
		try{
			HIDDeviceInfo[] devices = this.manager.listDevices();
			for(HIDDeviceInfo device : devices)
			{
				this.manager.openById(device.getVendor_id(),device.getProduct_id(),null).close(); 
			}
		}catch(IOException e){
			System.out.println("closeAllDevice() methode failed due to an IOException :\n" + e.getMessage());
		}
	}
	
	/**
	 *  close device with specific vid and pid value
	 * @param vid int vendor_id
	 * @param pid int product_id
	 * @author yassir
	 */
	public void closeDevice(int vid, int pid) {
		try {
			this.manager.openById(vid, pid, null).close();
		}catch (IOException e) {
			System.out.println("closeAllDevice(vid,pid) methode failed due to an IOException :\n" + e.getMessage());
		}
	}
	
	/**
	 * close a specific device 
	 * @param  device HIDDevice the device to close
	 * @author yassir
	 */
	public void closeDevice(HIDDevice device) {
		try {
			device.close();
		}catch (IOException e) {
			System.out.println("closeAllDevice(device) methode failed due to an IOException :\n" + e.getMessage());
		}
	}
	
	/**
	 *  close a specific device by his path
	 * @param path String device to close
	 * @author yassir
	 */
	public void closeDevice(String path) {
		try {
			this.manager.openByPath(path).close();
		} catch (IOException e) {
			System.out.println("closeAllDevice(path) methode failed due to an IOException :\n" + e.getMessage());
		}
	}
	
	/**
	 * close DaplugDongle
	 * @author yassir
	 */
	public void closeDevice() {
		try {
			this.manager.openById(VENDOR_ID, PRODUCT_ID, null).close();
		} catch (IOException e) {
			System.out.println("closeAllDevice() methode failed due to an IOException :\n" + e.getMessage());
		}
	}
	
	
	
	/**
	 *  return all informations of all HID DEVICE on the computer
	 * @return HIDDeviceInfo (is an array)
	 * @author yassir
	 */
	public HIDDeviceInfo[] listDevice(){
		 HIDDeviceInfo[] infos = null;
		 try {
	            infos = this.manager.listDevices();
	        } catch (Exception e) {
	        	System.out.println("listDevice methode failed :\n" + e.getMessage());
	        }
		 return infos;
	}
	
	/**
	 * Return specific Information form a specific HID DEVICE
	 * @param  vid int vendor_id of product
	 * @param  pid int product_id of product
	 * @return Vector<String>
	 * @author yassir
	 */
	public  Vector<HIDDeviceInfo> listDevice(int vid, int pid) {
		 HIDDeviceInfo[] infos = null;
		 Vector<HIDDeviceInfo> currentList = new Vector<HIDDeviceInfo>();
		 try {
	            infos =this.manager.listDevices();
	            for(HIDDeviceInfo deviceinfo : infos) {
	            	if (deviceinfo.getVendor_id() == vid && deviceinfo.getProduct_id() == pid)
	            		currentList.add(deviceinfo);
	            }
	        } catch (Exception e) {
	        	System.out.println("listDevice(vid,pid) methode failed :\n" + e.getMessage());
	 	       }
		 return currentList;
	}
	
	/**
	 * Return all Daplug or Plug-up device on computer
	 * @return Vector<HIDDeviceInfo> list 
	 * @author yassir
	 */
	public Vector<HIDDeviceInfo> listAllDaplug() {
		return this.listDevice(VENDOR_ID, PRODUCT_ID);
	}
}
