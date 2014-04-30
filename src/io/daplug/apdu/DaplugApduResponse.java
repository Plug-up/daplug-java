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
*/

package io.daplug.apdu;

import io.daplug.utils.DaplugUtils;

/**
 * An immutable DaplugApduResponse object.
 * Describes APDU response structure : returned data and SW. 
 * @author Saada
 *
 */
public final class DaplugApduResponse {

	private byte[] bytes;
	private byte[] data;
	private byte[] sw;
	private int sw1;
	private int sw2;
	private int dataLen;
	
	/**
	 * Constructs a new DaplugApduResponse object.
	 * @param bytes APDU response value as a bytes array.
	 * @throws Exception If an error occurs when constructing the object.
	 */
	public DaplugApduResponse(byte[] bytes) throws Exception{
		
		int len = bytes.length;
		
		if(len > DaplugApduCommand.APDU_DATA_MAX_LEN + 2) throw new Exception("Length exceeded !");
		if(len < 2) throw new Exception("Invalid Apdu response !");
		
		this.bytes = new byte[len];
		this.sw = new byte[2];
		this.dataLen = len-2;
		
		System.arraycopy(bytes, 0, this.bytes, 0, len);
		
		this.data = new byte[this.dataLen];
		System.arraycopy(this.bytes, 0, this.data, 0, this.dataLen);
				
		System.arraycopy(this.bytes, this.dataLen, this.sw, 0, 2);
		this.sw1 = (int) this.sw[0];
		this.sw2 = (int) this.sw[1];
	}
	
	/**
	 * Gets APDU response data length.
	 * @return APDU response data length.
	 */
	public int getDataLen(){
		return this.dataLen;
	}
	
	/**
	 * Gets APDU response value.
	 * @return APDU response value as a bytes-array.
	 */
	public byte[] getBytes(){
		return this.bytes;
	}
	
	/**
	 * Gets APDU response data.
	 * @return APDU response data as a bytes-array
	 */
	public byte[] getData(){		
		return this.data;
	}
	
	/**
	 * Gets APDU response status word.
	 * @return APDU response SW as a 2-bytes array
	 */
	public byte[] getSW(){
		return this.sw;		
	}
	
	/**
	 * Gets the first byte of the returned SW.
	 * @return first byte of the returned SW as an int value.
	 */
	public int getSW1(){
		return this.sw1;
	}
	
	/**
	 * Gets the second byte of the returned SW.
	 * @return second byte of the returned SW as an int value.
	 */
	public int getSW2(){
		return this.sw2;
	}
	
	/**
	 * Gets a hex string representation of the APDU response value
	 * @return APDU response value as a hex string.
	*/
	public String toString(){
		return DaplugUtils.byteArrayToHexString(this.bytes);
	}
	
	/**
	 * Indicates if the command associated to this response was successfully executed by the card.
	 * @return true if normal ending ; false otherwise.
	 */
	public boolean normalEnding(){
		
		if(this.sw1 == (byte)0x90 && this.sw2 == 0){
			return true;
		}else{
			return false;
		}
		
	}

}
