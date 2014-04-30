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
import io.daplug.exception.*;

public interface IHIDComm {
	
	 static final int BUFSIZE         = 2048;
	 static final int HID_BLOCK_SIZE  = 64;
	 static final int DEFAULT_TIMEOUT = 2000;
	 static final int STATUSWORD_DATA = 0x61;
	 
	 String[] exchange(byte[] apdu) throws DaplugException,
		DaplugCommunicationException,DaplugStatusWordException, IOException;
	 
	 String[] exchange(String hexaApdu) throws DaplugException,
		DaplugCommunicationException,DaplugStatusWordException, IOException;
	
}
