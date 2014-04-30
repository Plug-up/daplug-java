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

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.usb4java.*;
import io.daplug.utils.DaplugUtils;

public class WinusbComm implements IWinusb {

	private Device device = null;

	// device Handler
	private DeviceHandle dhandle = null;

	// device interface number
	private int deviceInterface;

	// EndpointIn formation
	private int output_endpoint;
	private int input_endpoint;
	
	@SuppressWarnings("unused")
	private int input_ep_max_packet_size;

	
	public WinusbComm(Device device) {
		this.device = device;
		this.dhandle = new DeviceHandle();
		this.initComm();
	}

	/**
	 * epic initComm : get element direction and endpoint from a device.
	 * inspiration from : C Daplug Api (author : Saada) and http://libusb.sourceforge.net/doc/examples-code.html
	 * @author yassir
	 */
	private void initComm() {
		DeviceDescriptor dev_desc = new DeviceDescriptor();
		ConfigDescriptor config_desc = new ConfigDescriptor();
		Interface interf = null;
		InterfaceDescriptor interf_desc = null;
		EndpointDescriptor ep_desc = null;

		// initilaize DeviceHandle and attach Device to his handler
		int result = LibUsb.open(this.device, this.dhandle);
		if (result != LibUsb.SUCCESS)
			throw new LibUsbException(
					"Unable to initialize Device. It may be null or do not exist",
					result);

		// initialize DeviceDescriptor and attach Device to his descriptor
		if ((result = LibUsb.getDeviceDescriptor(this.device, dev_desc)) != LibUsb.SUCCESS)
			throw new LibUsbException(
					"Unable to initialize Device Descpritor. It may be null or do not exist",
					result);
		boolean ep_out_ok = false;
		boolean ep_in_ok = false;
		
		// get for this device the number of all configuration possible
		for (int i = 0; i < dev_desc.bNumConfigurations(); i++) {
			
			// init config_desc, if failed, raised exception
			if ((result = LibUsb.getConfigDescriptor(this.device, (byte) i,
					config_desc)) != LibUsb.SUCCESS)
				throw new LibUsbException(
						"Unable to initialize ConfigDescriptor.\n It may be null or do not exist",
						result);
			for (int j = 0; j < config_desc.bNumInterfaces(); j++) {
				interf = config_desc.iface()[j];
				for (int k = 0; k < interf.numAltsetting(); k++) {
					interf_desc = interf.altsetting()[k];
					if (interf_desc.bInterfaceClass() != LibUsb.CLASS_VENDOR_SPEC)
						continue;
					
					// get Number of interface from InterfaceDescriptor
					this.deviceInterface = (int) interf_desc.bInterfaceNumber();

					for (int l = 0; l < interf_desc.bNumEndpoints(); l++) {
						ep_desc = interf_desc.endpoint()[l];
						boolean is_bulk = (ep_desc.bmAttributes() & LibUsb.TRANSFER_TYPE_MASK) == LibUsb.TRANSFER_TYPE_BULK;
						boolean is_out = (ep_desc.bEndpointAddress() & LibUsb.ENDPOINT_DIR_MASK) == LibUsb.ENDPOINT_OUT;
						boolean is_in = (ep_desc.bEndpointAddress() & LibUsb.ENDPOINT_DIR_MASK) == LibUsb.ENDPOINT_IN;
						if (is_bulk && is_out) {
							this.output_endpoint = ep_desc.bEndpointAddress();
							ep_out_ok = true;
						}
						if (is_bulk && is_in) {
							this.input_endpoint = ep_desc.bEndpointAddress();
							this.input_ep_max_packet_size = ep_desc
									.wMaxPacketSize();
							ep_in_ok = true;
						}
						if (ep_in_ok && ep_out_ok)
							break;
					}
					if (ep_in_ok && ep_out_ok)
						break;
				}
				if (ep_in_ok && ep_out_ok)
					break;
			}
			if (ep_in_ok && ep_out_ok)
				break;
		}
		LibUsb.freeConfigDescriptor(config_desc);
	}

	/**
	 * Proceed exchange data to WinUsb Dongle
	 * 
	 * @param apdu
	 *            byte []
	 * @return byte [] response
	 * @author yassir
	 */
	private String p_exchange(byte[] w_block) {

		// Claim interfaces to proceed to do write and read on winusb device
		// Check if kernel driver must be detached
		boolean detach = LibUsb
				.hasCapability(LibUsb.CAP_SUPPORTS_DETACH_KERNEL_DRIVER)
				&& (LibUsb.kernelDriverActive(this.dhandle,
						this.deviceInterface) != 0 ? true : false);

		// Detach the kernel driver
		if (detach) {
			int result = LibUsb.detachKernelDriver(this.dhandle,
					this.deviceInterface);
			if (result != LibUsb.SUCCESS)
				throw new LibUsbException("Unable to detach kernel driver",
						result);
		}
		// start communication with the device
		
		// send data to the device in bulk mode : use LibUsb.BulkTransfer to no deal with 
		// synchronus or asynchronus callback
		ByteBuffer w_buffer = BufferUtils.allocateByteBuffer(w_block.length);
		w_buffer.put(w_block);
		IntBuffer w_transferred = BufferUtils.allocateIntBuffer();
		int result_bulk = LibUsb.bulkTransfer(this.dhandle,
				(byte) this.output_endpoint, w_buffer, w_transferred, TIMEOUT);
		if (result_bulk != LibUsb.SUCCESS) {
			throw new LibUsbException(
					"Unable to send data : Control transfer failed",
					result_bulk);
		}

		// read from the device
		ByteBuffer r_buffer = BufferUtils.allocateByteBuffer(PACKET_SIZE);
		IntBuffer r_transferred = BufferUtils.allocateIntBuffer();
		int result_read = LibUsb.bulkTransfer(this.dhandle,
				(byte) this.input_endpoint, r_buffer, r_transferred, TIMEOUT);
		if (result_read != LibUsb.SUCCESS)
			throw new LibUsbException("Unable to read data", result_read);
		int value_received = r_transferred.get();

		// Attach the kernel driver again
		if (detach) {
			int result = LibUsb.attachKernelDriver(this.dhandle,
					this.deviceInterface);
			if (result != LibUsb.SUCCESS)
				throw new LibUsbException("Unable to re-attach kernel driver",
						result);
		}
		//get 
		byte [] f_result = new byte[r_buffer.capacity()];
		r_buffer.get(f_result, 0, f_result.length);
		String s_result = DaplugUtils.bytesToHex(f_result).substring(0, 2 * value_received);	
		return s_result;
	}
	
	/**
	 * make an exchange with a daplug dongle winusb. 
	 * return value is a String [] with 2 values :
	 * first the data received if exist
	 * second the status word
	 * @param apdu bute [] apdu to send to the dongle
	 * @return String[2] result
	 * @author yassir
	 */
	public String[] exchange(byte[] apdu) {
		String apdu_result = this.p_exchange(apdu);
		if (apdu_result.length() > 4) { // the result is compose of data + sw
			String[] result = new String[2];
			String status_word = apdu_result.substring(
					apdu_result.length() - 4, apdu_result.length());
			String tmp = apdu_result.substring(0, apdu_result.length() - 4);
			result[0] = tmp.substring(4, tmp.length());
			result[1] = status_word;
			return result;
		} else {
			String[] result = new String[2];
			result[0] = "";
			result[1] = apdu_result;
			return result;
		}
	}
	
}
