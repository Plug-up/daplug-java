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

package io.daplug.keyset;

//Host side keyset
/**
 * A host side keyset object. 
 * Its holds three DES keys. 
 * Each key is identified by its index: 0 (ENC key), 1 (MAC key) or 2 (DEK key).
 * ENC key is used for encryption and confidentiality. MAC key is used for integrity. DEK key is used for command data confidentiality in specific cases such as PUT KEY command.
 * The DaplugKeyset is associated with a usage and access control policy. The Key usage parameter defines what each key in the DaplugKeyset can be used for.
 * The first access value codes the time source DaplugKeyset version in case of TOTP DaplugKeyset. 
 * For all others DaplugKeyset roles, it codes the necessary access rights to be validated before being able to use a key in the DaplugKeyset : 0x00 for always, 0x01 to 0xFE for an access protected by a secure channel 0x01 to 0xFE.
 * The second access value codes decryption access in case of an ENC-DEC DaplugKeyset ; the key length in case of HMAC-SHA1, HOTP and TOTP Keysets ; the minimum security level in case of a GP DaplugKeyset.
 *
 * @author Saada
 *
 */
public class DaplugKeyset {
	
	public static final int GP_KEY_LEN = 16; //Global Platform key length
	
	//DaplugKeyset usage	
	public static final int 	USAGE_GP = 0x01, /** GlobalPlatform key. */
				    			USAGE_GP_AUTH = 0x02, /** GlobalPlatform key used for two-ways authentication */
							    USAGE_HOTP = 0x03, /** HOTP/OATH key */
							    USAGE_HOTP_VALIDATION = 0x04, /** HOTP/OATH key for validation. */
							    USAGE_TOTP_VALIDATION = 0x04, /** TOTP/OATH key for validation. */
							    USAGE_OTP = 0x05, /** RFU */
							    USAGE_ENC = 0x06, /** Encryption Key */
							    USAGE_DEC = 0x07, /** Decryption Key */
							    USAGE_ENC_DEC = 0x08, /** Encryption + Decryption key */
							    USAGE_SAM_CTX = 0x09, /** SAM context encryption key  */
							    USAGE_SAM_GP =  0x0A, /** SAM GlobalPlatform usable key  */
							    USAGE_SAM_DIV1 = 0x0B, /** SAM provisionable key with mandated diversification by at least one diversifier  */
							    USAGE_SAM_DIV2 = 0x0C, /** SAM provisionable key with mandated diversification by at least two diversifiers  */
							    USAGE_SAM_CLEAR_EXPORT_DIV1 = 0x0D, /** SAM cleartext exportable key with mandated diversification by at least one diversifier */
							    USAGE_SAM_CLEAR_EXPORT_DIV2 = 0x0E, /** SAM cleartext exportable key with mandated diversification by at least two diversifiers  */
							    USAGE_IMPORT_EXPORT_TRANSIENT = 0x0F, /** Transient keyset import/export key  */
							    USAGE_TOTP_TIME_SRC = 0x10, /** OATH TOTP time source key */
							    USAGE_TOTP = 0x11, /** TOTP/OATH key. */
							    USAGE_HMAC_SHA1 = 0x12 ,/** HMAC-SHA1 key. */
							    USAGE_HOTP_LOCK = 0x13, /** HOTP/OATH key locking the dongle after each use. */
							    USAGE_TOTP_LOCK = 0x14; /** TOTP/OATH key locking the dongle after each use. */
	
	private int version; //keyset version
	private int usage; //keyset role 
	private int access; //keyset access conditions as a 2-bytes int (Example. 0x0048) 
	private byte[] encKey; //ENC key value
	private byte[] macKey; //MAC key value
	private byte[] dekKey; //DEK key value
	
	/**
	 * Constructs a new empty DaplugKeyset object.
	 */
	public DaplugKeyset(){
		
		this.version = 0;
		this.usage = 0;
		this.access = 0;
		this.encKey = new byte[GP_KEY_LEN];
		this.macKey = new byte[GP_KEY_LEN];
		this.dekKey = new byte[GP_KEY_LEN];
	}
	
	/**
	 * Constructs a new DaplugKeyset object with the provided parameters.
	 * If the MAC or DEK key are not specified (null), the same ENC key value is used instead.
	 * @param version DaplugKeyset version.
	 * @param usage DaplugKeyset usage.
	 * @param access DaplugKeyset access condition as a 2-bytes int value (Example. 0x0048)
	 * @param encKey ENC key 16-bytes value
	 * @param macKey MAC key 16-bytes value
	 * @param dekKey DEK key 16-bytes value
	 */
	public DaplugKeyset(int version, int usage, int access, byte[] encKey, byte[] macKey, byte[] dekKey){
		
		this();
		this.version = version & 0xFF;
		this.usage = usage & 0xFF;
		this.access = access & 0xFFFF;
		System.arraycopy(encKey, 0, this.encKey, 0, GP_KEY_LEN);
		System.arraycopy(macKey, 0, this.macKey, 0, GP_KEY_LEN);
		System.arraycopy(dekKey, 0, this.dekKey, 0, GP_KEY_LEN);

	}
	
	/**
	 * 
	 * @param version DaplugKeyset version.
	 * @param encKey ENC key 16-bytes value
	 * @param macKey MAC key 16-bytes value
	 * @param dekKey DEK key 16-bytes value
	 */
	public DaplugKeyset(int version, byte[] encKey, byte[] macKey, byte[] dekKey){
		this(version, 0, 0, encKey, macKey, dekKey);
	}
	
	/**
	 * Constructs a new DaplugKeyset object with the provided parameters.
	 * The same ENC key value is used for MAC and DEK key values.
	 * @param version DaplugKeyset version.
	 * @param usage DaplugKeyset usage.
	 * @param access DaplugKeyset access condition as a 2-bytes int value (Example. 0x0048)
	 * @param key key value as a 16-bytes array.
	 */
	public DaplugKeyset(int version, int usage, int access, byte[] key){

		this(version, usage, access, key, key, key);

	}
	
	
	/**
	 * 
	 * @param version
	 * @param key Key value as a 16-bytes array
	 */
	public DaplugKeyset(int version, byte[] key){

		this(version, 0, 0, key);

	}

	
	/**
	 * Returns DaplugKeyset version.
	 * @return DaplugKeyset version.
	 */
	public int getVersion(){
		return this.version;
	}
	
	/**
	 * Returns DaplugKeyset role value.
	 * @return DaplugKeyset role value.
	 */
	public int getUsage(){
		return this.usage;
	}
	
	/**
	 * Returns the specified key value.
	 * @param index Key index in the DaplugKeyset. (0, 1 or 2)
	 * @return The specified key value.
	 */
	public byte[] getKey(int index) throws Exception{
		
		switch(index){
			case 0 : return this.encKey;
			case 1 : return this.macKey;
			case 2 : return this.dekKey;
			default : throw new Exception("Invalid index value : " + index);
		}
	}

	/**
	 * Returns the DaplugKeyset access conditions as a 2-bytes int value.
	 * @return DaplugKeyset access conditions.
	 */
	public int getAccess(){
		return this.access;
	}
	
	/**
	 * Sets DaplugKeyset version.
	 * @param version A new DaplugKeyset version.
	 */
	public void setVersion(int version){
		this.version = version & 0xFF;
	}

	/**
	 * Sets new DaplugKeyset usage.
	 */
	public void setUsage(int usage){
		this.usage = usage & 0xFF;
	}
	
	/**
	 * Sets new DaplugKeyset access.
	 * @param access New access conditions as a 2-bytes int value.
	 */
	public void setAccess(int access){
		this.access = access & 0xFFFF;
	}
	
	/**
	 * Sets a new key value in the DaplugKeyset. 
	 * @param index The index of the key to be modified in the DaplugKeyset.
	 * @param key A new key value as a 16-bytes array.
	 */
	public void setKey(int index, byte[] key){

		switch(index){
			case 0 : System.arraycopy(key, 0, this.encKey, 0, GP_KEY_LEN); break;
			case 1 : System.arraycopy(key, 0, this.macKey, 0, GP_KEY_LEN); break;
			case 2 : System.arraycopy(key, 0, this.dekKey, 0, GP_KEY_LEN); break;
			default : System.err.println("Invalid index value : " + index);
		}
	}
}


