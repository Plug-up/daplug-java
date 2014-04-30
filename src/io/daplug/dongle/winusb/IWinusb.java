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

public interface IWinusb {
	
	/**
	 * Daplug Winusb mode vendor_id
	 */
	final static short VENDOR_ID = 9601;
	
	/**
	 * Daplug Winusb mode product_id
	 */
	final static short PRODUCT_ID = 6152;
	
	/**
	 * Execution Time Out
	 */
	static final int TIMEOUT = 5000;
	
	static final int PACKET_SIZE = 512;
	
	
}
