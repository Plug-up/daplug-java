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

package io.daplug.keyboard;

public interface IKeyboard {

	final static int MAX_KB_CONTENT_SIZE  = 0xFFFF; // 65535 max int value
	final static int MAX_WINDOWS_TEXT_LEN = 0xFF;   // 255  per one TLV
	final static int MAX_MAC_TEXT_LEN     = 0xFC;   // 252 per one TLV (0xFF-3)
	
	final static int HOTP_USE_DIV 	  = 0x01; /**< Use diversifier */
	final static int HOTP_NUMERIC 	  = 0x02; /**< Use numeric output */
	final static int HOTP_MODHEX 	  = 0x04; /**< Use modified hex output */	
	
}

