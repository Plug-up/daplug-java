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
 * An immutable DaplugApduCommand object.
 * Describes APDU command structure : header and data. 
 * @author Saada
 *
 */
public final class DaplugApduCommand{
	
	public static final int APDU_HEADER_LEN = 5;
	public static final int APDU_DATA_MAX_LEN = 255;	
	public static final int APDU_COMMAND_MAX_LEN = APDU_HEADER_LEN + APDU_DATA_MAX_LEN;

	
	private int	cla,
				ins,
				p1,
				p2,
				lc, //Will be calculated automatically. You don't have to specify this parameter.
				le;
	
	private byte[] bytes;
	private byte[] header;
	private byte[] data;
	
	/**
	 * Constructs a new DaplugApduCommand object.
	 * @param bytes APDU command value as a bytes array.
	 * @throws Exception If an error occurs when constructing the object.
	 */
	public DaplugApduCommand(byte[] bytes) throws Exception{
		
		int len = bytes.length;
		
		if(len > APDU_COMMAND_MAX_LEN) throw new Exception("DaplugApduCommand() - Length exceeded : " + bytes.length);
		if(len < 5) throw new Exception("DaplugApduCommand() - Incomplete Apdu header : " + DaplugUtils.byteArrayToHexString(bytes));
		
		this.bytes = new byte[len];
		this.header = new byte[APDU_HEADER_LEN];
		this.data = new byte[len-APDU_HEADER_LEN];
		
		System.arraycopy(bytes, 0, this.bytes, 0, len);
		
		this.cla = this.bytes[0] & 0xFF;
		this.ins = this.bytes[1] & 0xFF;
		this.p1 = this.bytes[2] & 0xFF;
		this.p2 = this.bytes[3] & 0xFF;
			
		if(len == APDU_HEADER_LEN){
			this.lc = 0; 
			this.le = this.bytes[4] & 0xFF;
		}else{
			this.lc = len - APDU_HEADER_LEN;
			this.bytes[4] = (byte) this.lc;
			this.data = new byte[this.lc];
			System.arraycopy(this.bytes, APDU_HEADER_LEN, this.data, 0, this.lc);
		}
		
		System.arraycopy(this.bytes, 0, this.header, 0, APDU_HEADER_LEN);

	}
	
	/**
	 * Gets APDU command CLA value.
	 * @return CLA value.
	 */
	public int getCLA(){
		return this.cla;
	}
	
	/**
	 * Gets APDU command INS value.
	 * @return INS value.
	 */
	public int getINS(){
		return this.ins;
	}
	
	/**
	 * Gets APDU command P1 value.
	 * @return P1 value.
	 */
	public int getP1(){
		return this.p1;
	}
	
	/**
	 * Gets APDU command P2 value.
	 * @return P2 value.
	 */
	public int getP2(){
		return this.p2;
	}
	
	/**
	 * Gets APDU command data length.
	 * @return Lc value.
	 */
	public int getLc(){
		return this.lc;
	}
	
	/**
	 * Gets the expected length of APDU response data.
	 * Meaningful only if it is specified and if the APDU command don't contain data.
	 * @return Le value.
	 */
	public int getLe(){
		return this.le;
	}
	
	/**
	 * Gets APDU command header.
	 * @return APDU command header as a 5-bytes array.
	 */
	public byte[] getHeader(){
		return this.header;
	}
	
	
	/**
	 * Gets APDU command data.
	 * @return APDU command data as a bytes-array.
	 */
	public byte[] getData(){
		return this.data;
	}
	
	/**
	 * Gets APDU command value : header and data.
	 * @return APDU command value as a bytes-array.
	 */
	public byte[] getBytes(){
		return this.bytes;
	}	
	
	/**
	 * Gets a hex string representation of the APDU command value
	 * @return APDU command value as a hex string.
	 */
	public String toString(){		
		return DaplugUtils.byteArrayToHexString(this.bytes); 
	}
}
