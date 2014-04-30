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
*   Yassir Houssen Abdullah <a.yassirhoussen@plug-up.com>

*/

package io.daplug.session;

import java.util.Arrays;
import java.util.Date;
import java.util.Scanner;
import java.util.Vector;

import io.daplug.apdu.DaplugApduCommand;
import io.daplug.apdu.DaplugApduResponse;
import io.daplug.crypto.DaplugCrypto;
import io.daplug.dongle.DaplugDongle;
import io.daplug.dongle.DaplugEnumerator;
import io.daplug.keyset.DaplugKeyset;
import io.daplug.utils.DaplugUtils;

/**
 * This class provides a high level interface for communication with Daplug dongles. 
 * It maps the Daplug dongle specification in an user friendly format.
 * 
 * @author Saada
 *
 */
public final class DaplugSession {
	
	//Dongles
	public static final int	HID_DEVICE = 0,
							WINUSB_DEVICE = 1;

	//Security levels
    public static final int SEC_LEVEL_C_MAC = (byte) 0x01; /** Ensure command integrity */
    public static final int SEC_LEVEL_C_DEC = (byte) 0x02; /** Ensure command encryption */
    public static final int SEC_LEVEL_R_MAC = (byte) 0x10; /** Ensure response integrity */
    public static final int SEC_LEVEL_R_ENC = (byte) 0x20; /** Ensure response encryption */
    
    //Encryption constants
    public static final int	ENC_ECB = (byte) 0x01, /** Use ECB mode */
    	    				ENC_CBC = (byte) 0x02, /** Use CBC mode */
    	    				ENC_1_DIV = (byte) 0x04, /** Use one diversifier */
    	    				ENC_2_DIV = (byte) 0x08; /** Use two diversifiers */
    
    //OTP constants
    public static final int	OTP_6_DIGIT = (byte) 0x10, /** Output a 6-digits OTP */
    	    				OTP_7_DIGIT = (byte) 0x20, /** Output a 7-digits OTP */
    	    				OTP_8_DIGIT = (byte) 0x40, /** Output a 8-digits OTP */
    	    				OTP_0_DIV = (byte) 0x00, /** Do not use diversifiers */
    	    				OTP_1_DIV = (byte) 0x01, /** Use one diversifier */
    	    				OTP_2_DIV = (byte) 0x02; /** Use two diversifiers */
    						
    
    //FS
	public static final int MAX_FS_FILE_SIZE = 0xFFFF; //Max size of an EF
	public static final int MAX_REAL_DATA_SIZE = 0xEF; //EF = FF - 8 - 8 (data max len - possible mac - possible pad if data encrypted)
	public static final int FS_MASTER_FILE = 0x3F00; //Master file ID
	
    //Other constants
    public static final int MAC_LEN = 8; //MAC length
	public static final int HOTP_TIME_STEP = 30; //Recommended HOTP time step 
	public static final int ACCESS_ALWAYS = 0x00;
	public static final int ACCESS_NEVER = 0xFF;
	
	private static final int 	ENCRYPT = (byte) 0x01, /* Encryption */
								DECRYPT = (byte) 0x02, /* Decryption */
								HMAC = 0,
								HOTP = 1,
								TOTP = 2;
    
    private Vector<String> donglesList; //Daplug dongles list
	
	private DaplugDongle dongle; //A daplug dongle : HID/WINUSB mode
	
	private byte[]	cMac, /* Session command MAC */
					rMac, /* Session response MAC */
					sEncKey, /* Session command encryption key (used for confidentiality and authentication) */
			        rEncKey, /* Session response encryption key (used for confidentiality and authentication) */
			        cMacKey, /* Session command integrity key (used for integrity) */
			        rMacKey, /* Session response integrity key (used for integrity) */
			        sDekKey; /* Session DEK key (used for command data confidentiality in specific cases such as PUT KEY command) */

    private int securityLevel; /* Security level of the secure channel */
    private boolean session_opened; /* A flag indicating if a secure channel session is established or no. */
    
    /**
     * Constructs a new DaplugSession() object.
     * A DaplugSession is the main object. It contains informations about the selected Daplug dongle and the secure channel session.
     * @author Saada
     */
    public DaplugSession(){
    	
    	this.donglesList = null;
    	
    	this.dongle = null;
    	
    	this.cMac = new byte[MAC_LEN];  
    	this.rMac = new byte[MAC_LEN];
    	this.sEncKey = new byte[DaplugKeyset.GP_KEY_LEN];
    	this.rEncKey = new byte[DaplugKeyset.GP_KEY_LEN];
    	this.cMacKey = new byte[DaplugKeyset.GP_KEY_LEN];
    	this.rMacKey = new byte[DaplugKeyset.GP_KEY_LEN];
    	this.sDekKey = new byte[DaplugKeyset.GP_KEY_LEN];
    	
    	this.securityLevel = 0;
    	this.session_opened = false;
    }
    
    /**
     * Returns a list of connected Daplug dongles. This is an entry point into finding a Daplug dongle to operate.
     * @return Returns a list of connected Daplug dongles.
     * @throws Exception if an error occurs during Daplug dongles detection.
     * @author Saada
     */
    public Vector<String> getDonglesList() throws Exception{
    	
		this.donglesList = DaplugEnumerator.listDaplugDongles();
		
		if(this.donglesList.isEmpty()){
			throw new Exception("getDonglesList() - No dongle inserted !");
		}
		
		return this.donglesList;
    }
    
    /**
     * Makes available the requested Daplug dongle.
     * @param id A Daplug dongle index, chosen in the list returned by getDongleList().
     * @throws Exception if an error occurs when selecting the Daplug dongle.
     * @author Saada
     */
    public void getDongleById(int id) throws Exception{
    	
    	if(this.donglesList == null) throw new Exception("getDongleById() - Dongles list not initialized !");
    	
    	if(id < 0 || id > this.donglesList.size() - 1) throw new Exception("getDongleById() - Invalid id : " + id);			
    	
		this.dongle = new DaplugDongle(this.donglesList.elementAt(id));
		
    }
    
    /**
     * Makes available the first detected Daplug dongle.
     * @throws Exception if an error occurs when selecting the Daplug dongle.
     * @author Saada
     */
    public void getFirstDongle() throws Exception{
    	
    	getDongleById(0);
    }
    
    /**
     * Exchanges an Apdu command according to the security level of the secure channel. 
     * @param apdu The Apdu command to be exchanged.
     * @return the Apdu response.
     * @throws Exception if an error occurs during the exchange.
     * @author Saada
     */
    public DaplugApduResponse exchange(DaplugApduCommand apdu) throws Exception{
    	
    	//Wrap
    	DaplugApduCommand wrappedApdu = this.wrapApdu(apdu);
    	
    	
    	//Base exchange
    	String[] responseHexStr = new String[2];
    	
    	if(this.dongle != null){
    		try{
    			responseHexStr = this.dongle.exchange(wrappedApdu.toString());
    		}
    		catch(Exception e){
    			System.err.println(e.getMessage());
    		}
    	}else{
    		throw new Exception("Session dongle not initialized !");
    	}
    	
    	String resp = new String();
    	resp = resp.concat(responseHexStr[0]);
    	resp = resp.concat(responseHexStr[1]);
    	
    	byte[] responseBytes = DaplugUtils.hexStringToByteArray(resp);
    	
    	//Unwrap
    	DaplugApduResponse retResponse = null;
    	
    	try{
    		retResponse = this.unwrapApdu(apdu, responseBytes);
    	}catch(Exception e){
    		System.err.println(e.getMessage());
    	}   
    	
    	System.out.println("=> " + wrappedApdu.toString());
    	System.out.println("<= " + DaplugUtils.byteArrayToHexString(responseBytes));
    	
    	return retResponse;
    }
    
    /**
     * Get the unique serial number for the selected Daplug dongle.
     * @return Returned serial.
     * @author Saada
     */
    public byte[] getDongleSerial(){
    	
    	byte[] serial = null;
    	
    	try{
    		DaplugApduCommand a = new DaplugApduCommand(DaplugUtils.hexStringToByteArray("80E6000000"));
    		DaplugApduResponse r = this.exchange(a);
    		if(r.normalEnding()){
    			serial = r.getData();
    		}else{
    			throw new Exception("getDongleSerial() - Cannot retreive dongle serial !");
    		}
    	}catch(Exception e){
    		System.err.println(e.getMessage());
    	}
    	
    	return serial;
    
    }
    
    /**
     * Get the current status of the selected Daplug dongle.
     * @return Returned status (PERSONALIZED, TERMINATED, LOCKED)
     * @author Saada
     */
    public String getDongleStatus(){
    	
    	byte[] bytesBuf = null;
    	
    	try{
    		DaplugApduCommand a = new DaplugApduCommand(DaplugUtils.hexStringToByteArray("80F2400000"));
    		DaplugApduResponse r = this.exchange(a);
    		if(r.normalEnding()){
    			bytesBuf = r.getData();
    		}else{
    			throw new Exception("getDongleStatus() - Cannot retreive dongle status !");
    		}
    	}catch(Exception e){
    		System.err.println(e.getMessage());
    	}
    	
    	switch(bytesBuf[9]){
	    	case (byte) 0x0F :
	    		return "PERSONALIZED";
	    	case (byte) 0x7F :
	    		return "TERMINATED";
	    	case (byte) 0x83 :
	    		return "LOCKED";
    		default : 
    			return "INVALID STATUS";
    	}
    	
    }
    
    /**
     * Set a new status for the selected Daplug dongle.
     * @param status The new status.
     * @author Saada
     */
    public void setDongleStatus(int status){
    	
    	String apduStr = new String("80F040");
    	apduStr = apduStr.concat(String.format("%02X", status));
    	apduStr = apduStr.concat("00");
    	
    	try{
    		DaplugApduCommand a = new DaplugApduCommand(DaplugUtils.hexStringToByteArray(apduStr));
    		DaplugApduResponse r = this.exchange(a);
    		if(r.normalEnding()){
    			System.out.println("setDongleStatus() - Dongle status set");;
    		}else{
    			throw new Exception("setDongleStatus() - Cannot set status !");
    		}
    	}catch(Exception e){
    		System.err.println(e.getMessage());
    	}
    	
    }
    
    /**
     * Performs a mutual authentication between the Daplug dongle and a host client. 
     * A secure channel is then opened and used for exchanging Apdus acccording to the security level mode. 
     * Diversified DaplugKeyset is obtained by difersifying a master DaplugKeyset by the provided diversifier (computeDiversifiedKeys() can be used for obtaining a diversified DaplugKeyset).
     * If the diversifier is not provided (null), default authentication is used. 
     * The host challenge is used for computing the host cryptogram (a value used when performing the mutual authentication). If it is not provided (null), a random value is generated and used instead.
     * @param keys A master or a diversified DaplugKeyset.
     * @param mode Security level to use for authentication. 
     * The mode is a combination of possible values SEC_LEVEL_C_MAC, SEC_LEVEL_C_DEC, SEC_LEVEL_R_MAC, & SEC_LEVEL_R_ENC.
     * SEC_LEVEL_C_MAC is forced.
     * @param diversifier 16-bytes diversifier. Required only if a diversified Kayset is used.
     * @param challenge Optional host challenge.
     * @throws Exception if an error occurs when performing the mutual authentication.
     * @author Saada
     */
    public void authenticate(DaplugKeyset keys, int mode, byte[] diversifier, byte[] challenge) throws Exception{
    
    	byte[]	hostChallenge = new byte[8],
    			counter = new byte[2],
    			cardChallenge = new byte[6],
    			cardCryptogram = new byte[8],
    	    	hostCryptogram = new byte[8],
    			computedCardCryptogram = new byte[8];
    	
    	String temp = new String();
    	
    	DaplugApduCommand initializeUpdate = null,
    				externalAuthenticate = null;
    	
    	//Close Any SC previously opened
    	this.deAuthenticate();
    	
    	this.cMac = new byte[0]; //Force the first cMac to be empty (Not a 8-zero byte, but empty) 
    	
    	//Force mode to C_MAC if not set
    	if((mode & SEC_LEVEL_C_MAC) == 0){
    		mode = mode + SEC_LEVEL_C_MAC;
    	}
    	
    	if(challenge == null){
    		//generate host challenge
    		hostChallenge = DaplugCrypto.generateChallenge(8);
    	}else{
    		if(challenge.length != 8){
    			throw new Exception("authenticate() - Wrong challenge value : " + DaplugUtils.byteArrayToHexString(challenge));
    		}
    		System.arraycopy(challenge, 0, hostChallenge, 0, 8);
    	}
    	
    	//Any diversifier?
    	if(diversifier != null){
    		if(diversifier.length != 16){
    			throw new Exception("authenticate() - Wrong diversifier value : " + DaplugUtils.byteArrayToHexString(diversifier));
    		}
    	}
    	
    	//keyset version
    	String version = String.format("%02X", keys.getVersion());
    	
    	//Form the initialize apdu buf
    	if(diversifier == null){
    		temp = temp.concat("8050");
    		temp = temp.concat(version);
    		temp = temp.concat("0008");
    		temp = temp.concat(DaplugUtils.byteArrayToHexString(hostChallenge));
    	}else{
    		temp = temp.concat("D050");
    		temp = temp.concat(version);
    		temp = temp.concat("1018");
    		temp = temp.concat(DaplugUtils.byteArrayToHexString(hostChallenge));
    		temp = temp.concat(DaplugUtils.byteArrayToHexString(diversifier));
    	}
		try{
			initializeUpdate = new DaplugApduCommand(DaplugUtils.hexStringToByteArray(temp));
		}catch(Exception e){
			System.err.println(e.getMessage());
		}
		
		//Exchange
		DaplugApduResponse r = null;
		try{
			r = this.exchange(initializeUpdate);
		}catch(Exception e){
			System.err.println(e.getMessage());
		}
		
		if(!r.normalEnding()){
			throw new Exception("authenticate() - initialize update error !");
		}
		
		//Extract data returned by the card
		System.arraycopy(r.getData(), 12, counter, 0, counter.length);
		System.arraycopy(r.getData(), 12+counter.length, cardChallenge, 0, cardChallenge.length);
		System.arraycopy(r.getData(), 12+counter.length+cardChallenge.length, cardCryptogram, 0, cardCryptogram.length);
		
		//Compute session keys
		//Session s-enc key
		this.sEncKey = DaplugCrypto.computeSessionKey(counter, DaplugCrypto.KEY_CONSTANT_S_ENC, keys.getKey(0));
		//Session r-enc key
		this.rEncKey = DaplugCrypto.computeSessionKey(counter, DaplugCrypto.KEY_CONSTANT_R_ENC, keys.getKey(0));
		//Session c-mac key
		this.cMacKey = DaplugCrypto.computeSessionKey(counter, DaplugCrypto.KEY_CONSTANT_C_MAC, keys.getKey(1));
		//Session r-mac key
		this.rMacKey = DaplugCrypto.computeSessionKey(counter, DaplugCrypto.KEY_CONSTANT_R_MAC, keys.getKey(1));
		//Session dek key. In case of need it will be used. (to form "put key" command for example)
		this.sDekKey = DaplugCrypto.computeSessionKey(counter, DaplugCrypto.KEY_CONSTANT_DEK, keys.getKey(2));
		
		//Compute card cryptogram
		computedCardCryptogram = DaplugCrypto.computeCardCryptogram(hostChallenge, cardChallenge, counter, this.sEncKey);
		
		//Check card cryptogram
		if(DaplugCrypto.checkCardCryptogram(cardCryptogram, computedCardCryptogram) == false){
			throw new Exception("authenticate() - Card Cryptogram verification failed !");
		}else{			
			//Form the external authenticate apdu
			//compute host cryptogram
			hostCryptogram = DaplugCrypto.computeHostCryptogram(hostChallenge, cardChallenge, counter, this.sEncKey);
			//security level
			String secLevel = String.format("%02X", mode);
			
			temp = "";
			temp = temp.concat("8082");
			temp = temp.concat(secLevel);
			temp = temp.concat("0008");
			temp = temp.concat(DaplugUtils.byteArrayToHexString(hostCryptogram));
			
			try{
				externalAuthenticate = new DaplugApduCommand(DaplugUtils.hexStringToByteArray(temp));
			}catch(Exception e){
				System.err.println(e.getMessage());
			}
			
			//Exchange
			r = null;
			try{
				r = this.exchange(externalAuthenticate);
			}catch(Exception e){
				System.err.println(e.getMessage());
			}
			
			if(!r.normalEnding()){
				throw new Exception("authenticate() - external authenticate error !");
			}
			
		}
		
		System.out.println("authenticate() - Successful authentication...");		
		
		//Update session 
		System.arraycopy(this.cMac, 0, this.rMac, 0, MAC_LEN);
		this.securityLevel = mode;
		this.session_opened = true;
    }
    
    /**
     * Performs a mutual authentication between the Daplug dongle and a host client. 
     * A secure channel is then opened and used for exchanging Apdus acccording to the security level mode. 
     * @param keys A master DaplugKeyset.
     * @param mode Security level to use for authentication. 
     * The mode is a combination of possible values SEC_LEVEL_C_MAC, SEC_LEVEL_C_DEC, SEC_LEVEL_R_MAC, & SEC_LEVEL_R_ENC.
     * SEC_LEVEL_C_MAC is forced.
     * @throws Exception if an error occurs when performing the mutual authentication.
     * @author Saada
     */
    public void authenticate(DaplugKeyset keys, int mode) throws Exception{
    	authenticate(keys, mode, null, null);
    }
    
    /**
     * Diversifies a master DaplugKeyset.
     * @param keys A master DaplugKeyset.
     * @param diversifier 16-bytes diversifier.
     * @return The new DaplugKeyset.
     * @throws Exception if not a valid diversifier.
     * @author Saada
     */
    public DaplugKeyset computeDiversifiedKeys(DaplugKeyset keys, byte[] diversifier) throws Exception{
    	
    	DaplugKeyset divKeys = new DaplugKeyset();
    	
    	if(diversifier.length != 16){
    		throw new Exception("computeDiversifiedKeys() - Not a valid diversifier : " + DaplugUtils.byteArrayToHexString(diversifier));
    	}
    	
    	divKeys.setVersion(keys.getVersion());
    	divKeys.setUsage(keys.getUsage());
    	divKeys.setAccess(keys.getAccess());
    	divKeys.setKey(0, DaplugCrypto.computeDiversifiedKey(keys.getKey(0), diversifier));
    	divKeys.setKey(1, DaplugCrypto.computeDiversifiedKey(keys.getKey(1), diversifier));
    	divKeys.setKey(2, DaplugCrypto.computeDiversifiedKey(keys.getKey(2), diversifier));
    	
    	return divKeys;
    }
    
    /**
     * Closes the current Daplug session. 
     * @author Saada
     */
    public void deAuthenticate(){
    	
    	if(this.session_opened){
    		
    		//send Any Apdu to close the SC
    		try{
        		DaplugApduCommand a = new DaplugApduCommand(DaplugUtils.hexStringToByteArray("0000000000"));
        		DaplugApduResponse r = this.exchange(a);
        		if(!r.normalEnding()){
        			System.out.println("deAuthenticate() - De-authentication...");
        		}else{
        			throw new Exception("deAuthenticate() - De-authentication failed !");
        		}
        	}catch(Exception e){
        		System.err.println(e.getMessage());
        	}
    		
        	//this.donglesList = null;
        	//this.dongle = null; 		
    		this.securityLevel = 0;
    		this.session_opened = false;
    	}
    }
    
	/**
	 * Uploads the provided DaplugKeyset to the Daplug dongle. Can be used to upload a new DaplugKeyset or to modify an existing one.
	 * It is not possible to modify DaplugKeyset version, role, access or parent. Only keys values can be modified. 
	 * @param k The new DaplugKeyset.
	 * @param itselfParent When true, its indicates that the parent of the DaplugKeyset will be set to the DaplugKeyset itself. 
	 * if false, the parent of the DaplugKeyset will be set to the authenticated DaplugKeyset that created this new DaplugKeyset.
	 * The parent DaplugKeyset will be granted permission to delete the new DaplugKeyset.
     * @author Saada 
     */
    public void putKey(DaplugKeyset k, boolean itselfParent){
    	putKey(k, itselfParent, 0x81); //regular mode
    }
    
	/**
	 * Uploads the provided DaplugKeyset to the Daplug dongle. Can be used to upload a new DaplugKeyset or to modify an existing one.
	 * It is not possible to modify DaplugKeyset version, role, access or parent. Only keys values can be modified.
	 * @param itselfParent When true, its indicates that the parent of the DaplugKeyset will be set to the DaplugKeyset itself. 
	 * if false, the parent of the DaplugKeyset will be set to the authenticated DaplugKeyset that created this new DaplugKeyset.
	 * The parent DaplugKeyset will be granted permission to delete the new DaplugKeyset. 
	 * @param mode Two possible values : 0x81 (regular mode) and 0x82 (XOR mode).
	 * When operating in XOR mode and the DaplugKeyset already exists, the content of the keyset will be XORed by the provided content instead of being replaced.
     * @author Saada
	 */
    public void putKey(DaplugKeyset k, boolean itselfParent, int mode){
		
		byte[] retBuf = null;
		
		String 	element1 = "80D8",
				element2 = "", //numKeyset
				element3 = "", //mode
				element4 = "55", //Lc
				element5 = "FF8010", //key type + key length
				element6 = "", //GP-ENC key value, wrapped by session DEK
				element7 = "03", //KCV length
				element8 = "", //key1 KCV
				element9 = "01", //key usage length
				element10 = "", //key usage
				element11 = "02", //key access length
				element12 = "", //key access
				element13 = "", //GP-MAC key value, wrapped by session DEK
				element14 = "", //key2 kcv
				element15 = "", //GP-DEK value, wrapped by session DEK
				element16 = "", //key3 kcv
				element17 = ""; //keyset diversifier value for a GP keyset
		
		//This keyset is the parent of itself?
		int keysetUsage = 0;
		if(itselfParent){
			keysetUsage = (k.getUsage() + 0x80) & 0xFF;
		}else{
			keysetUsage = k.getUsage() & 0xFF;
		}
		
		element2 = String.format("%02X", k.getVersion());
		element3 = String.format("%02X", mode);
		element10 = String.format("%02X", keysetUsage);
		element12 = String.format("%04X", k.getAccess());
		
		//=== Encrypt GP keys ===
		
		try{
			//ENC key
			element6 = DaplugUtils.byteArrayToHexString(DaplugCrypto.tripleDES_ECB_GP(k.getKey(0), this.sDekKey, DaplugCrypto.ENCRYPT));
			//MAC key
			element13 = DaplugUtils.byteArrayToHexString(DaplugCrypto.tripleDES_ECB_GP(k.getKey(1), this.sDekKey, DaplugCrypto.ENCRYPT));
			//DEK key
			element15 = DaplugUtils.byteArrayToHexString(DaplugCrypto.tripleDES_ECB_GP(k.getKey(2), this.sDekKey, DaplugCrypto.ENCRYPT));
				
			//=== Compute KCVs ===
			//KCV1
			element8 = DaplugUtils.byteArrayToHexString(DaplugCrypto.computeKCV(k.getKey(0)));
			//KCV2
			element14 = DaplugUtils.byteArrayToHexString(DaplugCrypto.computeKCV(k.getKey(1)));
			//KCV3
			element16 = DaplugUtils.byteArrayToHexString(DaplugCrypto.computeKCV(k.getKey(2)));
		}catch(Exception e){
			System.err.println(e.getMessage());
		}
		
		//Form the putkey buf
		String hexStrBuf = "";
		hexStrBuf = hexStrBuf.concat(element1);
		hexStrBuf = hexStrBuf.concat(element2);
		hexStrBuf = hexStrBuf.concat(element3);
		hexStrBuf = hexStrBuf.concat(element4);
		hexStrBuf = hexStrBuf.concat(element2);
		hexStrBuf = hexStrBuf.concat(element5);
		hexStrBuf = hexStrBuf.concat(element6);
		hexStrBuf = hexStrBuf.concat(element7);
		hexStrBuf = hexStrBuf.concat(element8);
		hexStrBuf = hexStrBuf.concat(element9);
		hexStrBuf = hexStrBuf.concat(element10);
		hexStrBuf = hexStrBuf.concat(element11);
		hexStrBuf = hexStrBuf.concat(element12);
		hexStrBuf = hexStrBuf.concat(element5);
		hexStrBuf = hexStrBuf.concat(element13);
		hexStrBuf = hexStrBuf.concat(element7);
		hexStrBuf = hexStrBuf.concat(element14);
		hexStrBuf = hexStrBuf.concat(element9);
		hexStrBuf = hexStrBuf.concat(element10);
		hexStrBuf = hexStrBuf.concat(element11);
		hexStrBuf = hexStrBuf.concat(element12);
		hexStrBuf = hexStrBuf.concat(element5);
		hexStrBuf = hexStrBuf.concat(element15);
		hexStrBuf = hexStrBuf.concat(element7);
		hexStrBuf = hexStrBuf.concat(element16);
		hexStrBuf = hexStrBuf.concat(element9);
		hexStrBuf = hexStrBuf.concat(element10);
		hexStrBuf = hexStrBuf.concat(element11);
		hexStrBuf = hexStrBuf.concat(element12);
		hexStrBuf = hexStrBuf.concat(element17);
		
		retBuf = DaplugUtils.hexStringToByteArray(hexStrBuf);
		
    	try{
    		DaplugApduCommand a = new DaplugApduCommand(retBuf);
    		DaplugApduResponse r = this.exchange(a);
    		if(r.normalEnding()){
    			System.out.println("putKey() - DaplugKeyset " + String.format("%02X", k.getVersion()) + " successfuly created/modified");
    		}else{
    			throw new Exception("putKey() - Cannot create/modify keyset " + String.format("%02X", k.getVersion()) + " !");
    		}
    	}catch(Exception e){
    		System.err.println(e.getMessage());
    	}			
	}
    
    /**
     * Delete the specified DaplugKeyset.
     * @param keyVersion The version of an existing DaplugKeyset wich will be deleted.
     * @throws Exception if an error occurs when deleting the DaplugKeyset.
     * @author Saada
     */
    public void deleteKey(int keyVersion) throws Exception{
    	
    	String keyFileId = "10";
    	String keyFilePath  = "3F00:C00F:C0DE:0001:10";
    	
    	keyFileId = keyFileId.concat(String.format("%02X", keyVersion));
    	keyFilePath = keyFilePath.concat(String.format("%02X", keyVersion));
    	
    	this.selectPath(keyFilePath);
    	this.deleteFileOrDir(Integer.parseInt(keyFileId,16) & 0xFFFF);
    	
    	System.out.println("deleteKey() - Key " + String.format("%02X", keyVersion) + " successfully deleted...");
    	
    }
    
    /**
     * Exports the current transient keyset (0xF0) using the specified keyset version and key index.
     * A transient keyset (0xF0) is a virtual keyset located in RAM.. wich can be exported & imported. 
     * When exported, the keyset is encrypted with a transient export key (role 0x0F)..
     * @param keyVersion Version of the transient export keyset.
     * @param index Key index in the transient export keyset. Possible values are 1, 2 or 3.
     * @return The resultant encrypted keyset as a bytes blob.
     * @author Saada
     */
    public byte[] exportKey(int keyVersion, int index){
    	
    	byte[] exportedKey = null;
    	
    	//Form the export APDU
    	String hexStrBuf = "";
    	hexStrBuf = hexStrBuf.concat("D0A0");
    	hexStrBuf = hexStrBuf.concat(String.format("%02X", keyVersion));
    	hexStrBuf = hexStrBuf.concat(String.format("%02X", index));
    	hexStrBuf = hexStrBuf.concat("00");
    	
    	try{
    		DaplugApduCommand a = new DaplugApduCommand(DaplugUtils.hexStringToByteArray(hexStrBuf));
    		DaplugApduResponse r = this.exchange(a);
    		if(r.normalEnding()){
    			exportedKey = r.getData();
    			System.out.println("Key successfuly exported...");
    		}else{
    			throw new Exception("exportKey() - Cannot export key !");
    		}
    	}catch(Exception e){
    		System.err.println(e.getMessage());
    	}    	
    	
    	return exportedKey;
    }
    
    /**
     * Imports the provided transient keyset (0xF0) using the specified keyset version and key index.
     * A transient keyset (0xF0) is a virtual keyset located in RAM.. wich can be exported & imported. 
     * @param keyVersion Version of the transient export keyset.
     * @param index Key index in the transient export keyset. Possible values are 1, 2 or 3.
     * @param keyToImport An encrypted keyset previously exported with the exportKey() function. (as a bytes blob).
     * @author Saada
     */
    public void importKey(int keyVersion, int index, byte[] keyToImport){
    	
    	//Form the export APDU
    	String hexStrBuf = "";
    	hexStrBuf = hexStrBuf.concat("D0A2");
    	hexStrBuf = hexStrBuf.concat(String.format("%02X", keyVersion));
    	hexStrBuf = hexStrBuf.concat(String.format("%02X", index));
    	hexStrBuf = hexStrBuf.concat(String.format("%02X", keyToImport.length));
    	hexStrBuf = hexStrBuf.concat(DaplugUtils.byteArrayToHexString(keyToImport));
    	
    	try{
    		DaplugApduCommand a = new DaplugApduCommand(DaplugUtils.hexStringToByteArray(hexStrBuf));
    		DaplugApduResponse r = this.exchange(a);
    		if(r.normalEnding()){
    			System.out.println("Key successfuly imported...");
    		}else{
    			throw new Exception("importKey() - Cannot import key !");
    		}
    	}catch(Exception e){
    		System.err.println(e.getMessage());
    	}    	
    }    
    
    /**
     * Creates a new file with the given ID (0 to 65535) and the given size (1 to 65535) under the current directory. Access conditions are specified as a three-value int array.
     * The first value codes the DELETE access condition. The second value codes the UPDATE access condtion. The third value codes the READ access condition. 
     * An access condition is coded as follows : 0x00 for always, 0xFF for never, 0x01 to 0xFE for an access protected by a secure channel 0x01 to 0xFE. 
     * A counter file size shall be 8 bytes.
     * @param id A file ID.
     * @param size The file size.
     * @param access Access conditions.
     * @param isEncFile Indicates if the created file content will be encrypted (true) or not (false).
     * @param isCntFile Indicates if the created file is a counter file (true) or not (false).
     * @throws Exception if an error occurs when creating the file.
     * @author Saada
     */
    public void createFile(int id, int size, int[] access, boolean isEncFile, boolean isCntFile) throws Exception{
    	
    	if(access.length != 3){
    		throw new Exception("createFile() - Invalid access value !");
    	}
    	    	
    	int ief, icf;

    	if(isCntFile){ //counter file
    		size = 8; //A counter file shall be created with a length set to 8 bytes
    		icf = 1;
    	}else{
    		icf = 0;
    	}
    	
    	if(isEncFile){ //encrypted file
    		ief = 1;
    	}else{
    		ief = 0;
    	}    	
    	
    	//Form the APDU
    	String hexStrBuf = "";
    	hexStrBuf = hexStrBuf.concat("80E000001C6214820201218302");
    	hexStrBuf = hexStrBuf.concat(String.format("%04X", id));
    	hexStrBuf = hexStrBuf.concat("8102");
    	hexStrBuf = hexStrBuf.concat(String.format("%04X", size));
    	hexStrBuf = hexStrBuf.concat("8c0600");
    	hexStrBuf = hexStrBuf.concat(String.format("%02X", access[0]));
    	hexStrBuf = hexStrBuf.concat("0000");
    	hexStrBuf = hexStrBuf.concat(String.format("%02X", access[1]));
    	hexStrBuf = hexStrBuf.concat(String.format("%02X", access[2]));
    	hexStrBuf = hexStrBuf.concat("8601");
    	hexStrBuf = hexStrBuf.concat(String.format("%02X", ief));
    	hexStrBuf = hexStrBuf.concat("8701");
    	hexStrBuf = hexStrBuf.concat(String.format("%02X", icf));
    	
    	try{
    		DaplugApduCommand a = new DaplugApduCommand(DaplugUtils.hexStringToByteArray(hexStrBuf));
    		DaplugApduResponse r = this.exchange(a);
    		if(r.normalEnding()){
    			System.out.println("createFile() - File " + String.format("%04X", id) + " successfuly created...");
    		}else{
    			throw new Exception("createFile() - Cannot create file " + String.format("%04X", id) + " !");
    		}
    	}catch(Exception e){
    		System.err.println(e.getMessage());
    	}    	
    }
    
    /**
     * Creates a new directory with the given ID (0 to 65535) under the current directory. Access conditions are specified in a three-value int array.
 	 * The first value codes the DELETE SELF access condition. The second value codes the CREATE DIRECTORY access condition. The third value codes the CREATE FILE access condition. 
 	 * An access condition is coded as follows : 0x00 for always, 0xFF for never, 0x01 to 0xFE for an access protected by a secure channel 0x01 to 0xFE.
     * @param id A directory ID.
     * @param access Access conditions.
     * @throws Exception if an error occurs when creating the directory.
     * @author Saada
     */
    public void createDir(int id, int[] access) throws Exception{
    	
    	if(access.length != 3){
    		throw new Exception("createFile() - Invalid access value !");
    	}
    	
    	//Form the APDU
    	String hexStrBuf = "";
    	hexStrBuf = hexStrBuf.concat("80E0000010620E820232218302");
    	hexStrBuf = hexStrBuf.concat(String.format("%04X", id));
    	hexStrBuf = hexStrBuf.concat("8C0400");
    	hexStrBuf = hexStrBuf.concat(String.format("%02X", access[0]));
    	hexStrBuf = hexStrBuf.concat(String.format("%02X", access[1]));
    	hexStrBuf = hexStrBuf.concat(String.format("%02X", access[2]));
    	
    	try{
    		DaplugApduCommand a = new DaplugApduCommand(DaplugUtils.hexStringToByteArray(hexStrBuf));
    		DaplugApduResponse r = this.exchange(a);
    		if(r.normalEnding()){
    			System.out.println("createDir() - Directory " + String.format("%04X", id) + " successfuly created...");
    		}else{
    			throw new Exception("createFile() - Cannot create directory " + String.format("%04X", id) + " !");
    		}
    	}catch(Exception e){
    		System.err.println(e.getMessage());
    	}

    }
    
    /**
     * Deletes the specified file or directory.
     * @param id A file/directory ID.
     * @author Saada
     */
    public void deleteFileOrDir(int id){
    	
    	String hexStrBuf = "";
    	hexStrBuf = hexStrBuf.concat("80E4000002");
    	hexStrBuf = hexStrBuf.concat(String.format("%04X", id));
    	
    	try{
    		DaplugApduCommand a = new DaplugApduCommand(DaplugUtils.hexStringToByteArray(hexStrBuf));
    		DaplugApduResponse r = this.exchange(a);
    		if(r.normalEnding()){
    			System.out.println("deleteFileOrDir() - File/Directory " + String.format("%04X", id) + " successfuly deleted...");
    		}else{
    			throw new Exception("deleteFileOrDir() - Cannot delete file/directory " + String.format("%04X", id) + " !");
    		}
    	}catch(Exception e){
    		System.err.println(e.getMessage());
    	}
    	
    }
    
    /**
     * Selects the specified file.
     * @param id A file/directory ID.
     * @author Saada
     */
    public void selectFile(int id){
    	
    	String hexStrBuf = "";
    	hexStrBuf = hexStrBuf.concat("80A4000002");
    	hexStrBuf = hexStrBuf.concat(String.format("%04X", id));
    	
    	try{
    		DaplugApduCommand a = new DaplugApduCommand(DaplugUtils.hexStringToByteArray(hexStrBuf));
    		DaplugApduResponse r = this.exchange(a);
    		if(r.normalEnding()){
    			System.out.println("selectFile() - File " + String.format("%04X", id) + " seleted...");
    		}else{
    			throw new Exception("selectFile() - Cannot select file " + String.format("%04X", id) +" !");
    		}
    	}catch(Exception e){
    		System.err.println(e.getMessage());
    	}
    	
    }
    
    /**
	 * Selects the specified path. 
	 * A path is specified as a string containing a sequence of files/directories IDs separated by a colon. Each file ID is specified as two bytes hexstring.
     * For example, to select the file 0x0036 located under the directory 0x2214 located under the master file (0x3F00), use path "3F00:2214:0036".
     * @param path Path to select.
     * @throws Exception if an error occurs when selecting the path.
     * @author Saada
     */
    public void selectPath(String path) throws Exception{
    	
		Scanner s = new Scanner(path);
    	s.useDelimiter(":");
    	
    	while(s.hasNext()){
    		int id = s.nextInt(16);
    		selectFile(id);    		
    	}
    }
    
    /**
     * Reads length bytes of data from the selected file. Reading starts at the given offset.
     * @param offset Indicates where reading position starts.
     * @param length The length of data to read.
     * @return Read data.
     * @throws Exception if an error occurs when reading.
     * @author Saada
     */
    public byte[] readData(int offset, int length) throws Exception{
    	
    	byte[] readData = new byte[length];
    	
    	int lastPartLen = length % MAX_REAL_DATA_SIZE;
    	
    	if(length + offset > MAX_FS_FILE_SIZE){
    		readData = null;
    		throw new Exception("readData() - Authorized data length exceeded !");
    	}
    	
    	int readsNb = 0, i = 0;
    	if(lastPartLen == 0) readsNb = length/MAX_REAL_DATA_SIZE; else readsNb = (int)length/MAX_REAL_DATA_SIZE + 1; 
    	
    	while(readsNb > 0){
    		    		
    		String hexStrBuf = "";
        	hexStrBuf = hexStrBuf.concat("80B0");
        	hexStrBuf = hexStrBuf.concat(String.format("%04X", offset));
        	hexStrBuf = hexStrBuf.concat("00");
        	
        	try{
        		DaplugApduCommand a = new DaplugApduCommand(DaplugUtils.hexStringToByteArray(hexStrBuf));
        		DaplugApduResponse r = this.exchange(a);
        		if(r.normalEnding()){
        			int len = 0;
        			if(readsNb == 1 && lastPartLen != 0){
        				len = lastPartLen; 
        			}else{
        				len = MAX_REAL_DATA_SIZE;
        			}
        			if(r.getDataLen() < len){
        				readData = null;
        				throw new Exception("readData() -  The requested length exceeds file's size !");
        			}
            		System.arraycopy(r.getData(), 0, readData, i * MAX_REAL_DATA_SIZE, len);
        		}else{
        			readData = null;
        			throw new Exception("readData() - Data read failed !");
        		}
        	}catch(Exception e){
        		System.err.println(e.getMessage());
        	}
        	
        	offset = offset + MAX_REAL_DATA_SIZE;
        	i++;
    		readsNb--;
    		
    	}   	
    	
    	return readData;
    }
    
    /**
     * Writes provided data into the selected file. Writing starts at the given offset.
     * @param offset Indicates where writing position starts.
     * @param dataToWrite Data to write.
     * @throws Exception if an error occurs when writing data.
     * @author Saada
     */
    public void writeData(int offset, byte[] dataToWrite) throws Exception{
    	
    	int len = dataToWrite.length;
    	int lastPartLen = len % MAX_REAL_DATA_SIZE;
    	byte[] tempDataBuf = null;
    	
    	if(len + offset > MAX_FS_FILE_SIZE){
    		throw new Exception("writeData() - Authorized data length exceeded !");
    	}
    	
    	int writeNb = 0, i = 0;
    	if(lastPartLen == 0) writeNb = len/MAX_REAL_DATA_SIZE; else writeNb = (int)len/MAX_REAL_DATA_SIZE + 1;    	
    	
    	while(writeNb > 1 || (writeNb == 1 && lastPartLen == 0)){
    		
    		tempDataBuf = new byte[MAX_REAL_DATA_SIZE];
    		System.arraycopy(dataToWrite, i * MAX_REAL_DATA_SIZE, tempDataBuf, 0, tempDataBuf.length);
    		
    		String hexStrBuf = "";
        	hexStrBuf = hexStrBuf.concat("80D6");
        	hexStrBuf = hexStrBuf.concat(String.format("%04X", offset));
        	hexStrBuf = hexStrBuf.concat(String.format("%02X", MAX_REAL_DATA_SIZE));
        	hexStrBuf = hexStrBuf.concat(DaplugUtils.byteArrayToHexString(tempDataBuf));
        	
        	try{
        		DaplugApduCommand a = new DaplugApduCommand(DaplugUtils.hexStringToByteArray(hexStrBuf));
        		DaplugApduResponse r = this.exchange(a);
        		if(!r.normalEnding()){
        			throw new Exception("writeData() - Data write failed !");
        		}
        	}catch(Exception e){
        		System.err.println(e.getMessage());
        	}
        	
        	offset = offset + MAX_REAL_DATA_SIZE;
        	i++;
    		writeNb--;
    		
    	}
    	
    	//Write the last part : here last part length is < MAX_REAL_DATA_SIZE
    	tempDataBuf = new byte[lastPartLen];
		System.arraycopy(dataToWrite, i * MAX_REAL_DATA_SIZE, tempDataBuf, 0, tempDataBuf.length);
		
		String hexStrBuf = "";
    	hexStrBuf = hexStrBuf.concat("80D6");
    	hexStrBuf = hexStrBuf.concat(String.format("%04X", offset));
    	hexStrBuf = hexStrBuf.concat(String.format("%02X", lastPartLen));
    	hexStrBuf = hexStrBuf.concat(DaplugUtils.byteArrayToHexString(tempDataBuf));
    	
    	try{
    		DaplugApduCommand a = new DaplugApduCommand(DaplugUtils.hexStringToByteArray(hexStrBuf));
    		DaplugApduResponse r = this.exchange(a);
    		if(!r.normalEnding()){
    			throw new Exception("writeData() - Data write failed !");
    		}
    	}catch(Exception e){
    		System.err.println(e.getMessage());
    	}
    	
    }
    
	/**
	 * Encrypts a sequence of bytes using Triple DES encryption. Clear data length must be a multiple of 8 bytes. 
	 * The mode parameter combines options to use such as block cipher mode (ENC_ECB or ENC_CBC) and the use of diversifiers or not (ENC_1_DIV, ENC_2_DIV).
 	 * For example, if we want to use CBC with two provided diversifiers, mode must be equal to ENC_CBC + ENC_2_DIV. If not provided, optional parameters must be specified as null.
	 * @param keyVersion Encryption DaplugKeyset version.
	 * @param keyId The index of the keyset key to use for encryption (1,2 or 3).
	 * @param mode Specifies block cipher mode (ECB or CBC) and if we use diversifiers or not.
	 * @param iv initialization vector (Used only for CBC mode). If not specified, a zero-IV is used instead.
	 * @param div1 First diversifier (optional).
	 * @param div2 Second diversifier (optional).
	 * @param clearData sequence of bytes to encrypt.
	 * @return Resultant encrypted data.
	 * @throws Exception if an error occurs during encryption.
     * @author Saada
	 */
    public byte[] encrypt(int keyVersion, int keyId, int mode, byte[] iv, byte[] div1, byte[] div2, byte[] clearData) throws Exception{
    	return crypt(keyVersion, keyId, mode, iv, div1, div2, clearData, ENCRYPT);
    }
    
    /**
     * Decrypts a sequence of bytes previously encrypted using Triple DES encryption. Encrypted Data length must be a multiple of 8 bytes. 
     * The mode parameter combines options to use such as block cipher mode (ENC_ECB or ENC_CBC) and the use of diversifiers or not (ENC_1_DIV, ENC_2_DIV).
 	 * For example, if we want to use CBC with two provided diversifiers, mode must be equal to ENC_CBC + ENC_2_DIV. If not provided, optional parameters must be specified as null;
	 * @param keyVersion Encryption DaplugKeyset version.
	 * @param keyId The index of the keyset key to use for encryption (1,2 or 3).
	 * @param mode Specifies block cipher mode (ECB or CBC) and if we use diversifiers or not.
	 * @param iv initialization vector (Used only for CBC mode). If not specified, a zero-IV is used instead.
	 * @param div1 First diversifier (optional).
	 * @param div2 Second diversifier (optional).
	 * @param encryptedData sequence of bytes to decrypt (peviously encrypted using the same parameters).
	 * @return Resultant encrypted data.
	 * @throws Exception if an error occurs during encryption.
     * @author Saada
     */
    public byte[] decrypt(int keyVersion, int keyId, int mode, byte[] iv, byte[] div1, byte[] div2, byte[] encryptedData) throws Exception{
    	return crypt(keyVersion, keyId, mode, iv, div1, div2, encryptedData, DECRYPT);
    }
    
    /**
	 * make the daplug dongle generate a true random value.
	 * @param length int : the length of random value desire
	 * @return byte [] array
	 * @throws Exception
	 * @author yassir
	 */
	public byte[] getRandom(int length) throws Exception{
		String s_apdu = "D0240000";
		if (length <= 0 || length > MAX_REAL_DATA_SIZE)
			throw new Exception("getRandom() : Invalid random length ! Correct length is between 1 and 239 bytes");
		String len_s = String.format("%02X", length);
		StringBuilder sb = new StringBuilder();
		sb.append(s_apdu).append(len_s);
		//the length here is the returned data length (the apdu does not contain input data)
	    //for wrap reason, we use non meaningful data with size Lc
		int i = length;
		while (i > 0) {
			sb.append("00");
			i--;
		}
		DaplugApduCommand apdu = new DaplugApduCommand(DaplugUtils.hexStringToByteArray(sb.toString()));
		DaplugApduResponse result = this.exchange(apdu);
		if (result.normalEnding() == false)
			throw new Exception("getRandom() : Can not generate Ramdom value! ");
		return result.getData();
	}
	
    /**
     * Signs provided data using HMAC-SHA1. The resultant data is an 20-bytes signature. options parameter specifies if we want to use one (OTP_1_DIV) or two (OTP_2_DIV) provided diversifier(s).
	 * If no diversifier is provided, div parameter must be equal to an empty string ("") and mode parameter must be equal to OTP_0_DIV.
	 * @param keysetVersion int HMAC keyset version (see x_otp)
	 * @param options int Specifies if we want to use diversifiers or not
	 * @param div1 byte [] First diversifier
	 * @param div2 byte [] Second diversifier
	 * @param inData byte [] Data to sign
	 * @return result byte [] HHMAC-SHA-1 20 bytes signature
	 * @throws Exception
	 * @author yassir
     */
    public byte[] hmac(int keysetVersion, int options, byte[] div1, byte[] div2, byte[] inData) throws Exception{
    	
    	if (((options & OTP_6_DIGIT) != 0) && ((options & OTP_7_DIGIT) != 0) && ((options & OTP_8_DIGIT) != 0)){
    		throw new Exception ("hmac() - Invalid option for hmac : " + options);
    	}	
    	
    	return hmacSha1(keysetVersion, options, div1, div2, inData, HMAC);
    }
    
    /**
     * Returns an HMAC based One Time Password. options parameter specifies the size of the resultant HOTP and if we want to use one (OTP_1_DIV) or two (OTP_2_DIV) provided diversifier(s).
	 * If no diversifier is provided, div parameters must be equal to an empty string ("") and mode parameter must be equal to OTP_0_DIV.
	 * @param keysetVersion int HOTP/HOTP_VALIDATION keyset version
	 * @param options int Specifies Specifies the size of the resultant HOTP and if we want to use diversifiers or not
	 * @param div1 byte [] First diversifier
	 * @param div2 byte [] Second diversifier
	 * @param inData byte [] A counter file ID if HOTP keyset is provided or counter value as an 8 bytes string if HOTP_VALIDATION keyset is provided.
	 * @return result byte[] HMAC based One Time Password.
	 * @author yassir
     */
    public byte[] hotp(int keysetVersion, int options, byte[] div1, byte[] div2, byte[] inData) throws Exception{
    	
		if (((options & OTP_6_DIGIT) == 0) && ((options & OTP_7_DIGIT) == 0) && ((options & OTP_8_DIGIT) == 0)){
			throw new Exception ("hotp() : Invalid mode option for hotp : " + options);
		}
		
		if ((inData.length != 2) && (inData.length != 8)){
			throw new Exception ("hotp() : Invalid Data for hotp : " + DaplugUtils.byteArrayToHexString(inData));
		}
    	
		return hmacSha1(keysetVersion, options, div1, div2, inData, HOTP);
    }
    
	/**
	 * Returns a Time based One Time Password. options parameter specifies the size of the resultant HOTP and if we want to use one (OTP_1_DIV) or two (OTP_2_DIV) provided diversifier(s).
	 * If no diversifier is provided, div parameters must be equal to an empty string ("") and mode parameter must be equal to OTP_0_DIV. If TOTP keyset is provided,
	 * Daplug_setTimeOTP() function must have been called with a time source key matching the key requirement.
	 * @param keysetVersion int TOTP/TOTP_VALIDATION keyset version
	 * @param options int Specifies the size of the resultant TOTP and if we want to use diversifiers or not
	 * @param div1 byte [] First diversifier
	 * @param div2 byte [] Second diversifier
	 * @param inData byte [] Empty value "" if TOTP keyset is provided or time data as an 8 bytes string if TOTP_VALIDATION keyset is provided.
	 * @return result String Time based One Time Password.
	 * @throws Exception
	 * @author yassir
	 */
    public byte[] totp(int keysetVersion, int options, byte[] div1, byte[] div2, byte[] inData) throws Exception{
    	
		if (((options & OTP_6_DIGIT) == 0)  && ((options & OTP_7_DIGIT) == 0) && ((options & OTP_8_DIGIT) == 0))
		throw new Exception ("totp() : Invalid mode option for Daplug_totp : " + options);
	
		if ((inData != null) && (inData.length != 8)){
			throw new Exception ("totp() - Invalid Data for totp : " + DaplugUtils.byteArrayToHexString(inData));
		}
		
    	return hmacSha1(keysetVersion, options, div1, div2, inData, TOTP);
    }
  
	/**
	 * Sets the time reference of the dongle. After the time reference is set, the dongle internal clock will tick from this value on until it is powered off.
	 * If step parameter is not specified (=0), a typical value is used (30).
	 * If t parameter is not specified, system time is used.
	 * @param keysetVersion int Time source keyset version
	 * @param keyId int ID to use in the keyset
	 * @param timeSrcKey int  The Time source key identified by the keyId
	 * @param step int  TOTP Time Step (optional = 0)
	 * @param t int  Time value (optional = 0)
	 * @return result String
	 * @throws Exception
	 * @author yassir
	 */
	public String setTimeOTP(int keysetVersion, int keyId,
			byte [] timeSrcKey, int step, int t) throws Exception {
		String initial_apdu = "D0B2";
		String s_keyVersion = String.format("%02X", keysetVersion);
		String s_keyId = String.format("%02X", keyId);

		// Signature
		String temp_in = "", temp_out = "", signature = "";
		byte[] nonce = DaplugCrypto.generateChallenge(11);
		String s_nonce = DaplugUtils.byteArrayToHexString(nonce);
		
		if (step == 0)
			step = HOTP_TIME_STEP;
		String s_step = String.format("%02X", step);

		// get currentTime in millisecondes if t = 0
		if (t == 0)
			t = (int) (new Date().getTime());

		String time_s = String.format("%08X", t);
		
		StringBuilder sb1 = new StringBuilder();
		sb1.append(s_nonce).append(s_step).append(time_s);
		temp_in = sb1.toString();
		
		// tripleDES_CBC
		temp_out = DaplugUtils.bytesToHex(DaplugCrypto.tripleDES_CBC_GP(
				DaplugUtils.hexStringToByteArray(temp_in),
				timeSrcKey, null, 1));
		
		signature = temp_out.substring(16, 32);
		
		// form the apdu
		StringBuilder sb = new StringBuilder();
		sb.append(initial_apdu).append(s_keyVersion).append(s_keyId)
				.append("18").append(temp_in).append(signature);
		String output = sb.toString();

		// Set to apdu cde
		DaplugApduCommand apduCommand = new DaplugApduCommand(
				DaplugUtils.hexStringToByteArray(output));
		DaplugApduResponse response = this.exchange(apduCommand);
		if (response.normalEnding() == false)
			return "setTimeOTP(): Cannot set time reference for dongle !";
		else
			return "setTimeOTP(): Dongle_info time reference set.";
	}
	
	/**
	 * Sets the time reference of the dongle. After the time reference is set, the dongle internal clock will tick from this value on until it is powered off.
	 * If step parameter is not specified (=0), a typical value is used (30). the system time source wil be used.
	 * @param keysetVersion int Time source keyset version
	 * @param keyId int id to use in the keyset
	 * @param timeSrcKey int  The Time source key identified by the keyId
	 * @return result String
	 * @throws Exception
	 * @author yassir
	 */
	public String  setTimeOTP(int keysetVersion, int keyId,
			byte [] timeSrcKey) throws Exception {
		String initial_apdu = "D0B2";
		String s_keyVersion = String.format("%02X", keysetVersion);
		String s_keyId = String.format("%02X", keyId);

		// Signature
		String temp_in = "", temp_out = "", signature = "";
		byte[] nonce = DaplugCrypto.generateChallenge(11);
		String s_nonce = DaplugUtils.byteArrayToHexString(nonce);
		
		
		int step = HOTP_TIME_STEP;
		String s_step = String.format("%02X", step);

		// get currentTime in millisecondes if t = 0
	
		int t = (int) (new Date().getTime());

		String time_s = String.format("%08X", t);
		
		StringBuilder sb1 = new StringBuilder();
		sb1.append(s_nonce).append(s_step).append(time_s);
		temp_in = sb1.toString();
		
		// tripleDES_CBC
		temp_out = DaplugUtils.bytesToHex(DaplugCrypto.tripleDES_CBC_GP(
				DaplugUtils.hexStringToByteArray(temp_in),
				timeSrcKey, null, 1));
		
		signature = temp_out.substring(16, 32);
		
		// form the apdu
		StringBuilder sb = new StringBuilder();
		sb.append(initial_apdu).append(s_keyVersion).append(s_keyId)
				.append("18").append(temp_in).append(signature);
		String output = sb.toString();

		// Set to apdu cde
		DaplugApduCommand apduCommand = new DaplugApduCommand(
				DaplugUtils.hexStringToByteArray(output));
		DaplugApduResponse response = this.exchange(apduCommand);
		if (response.normalEnding() == false)
			return "setTimeOTP(): Cannot set time reference for dongle !";
		else
			return "setTimeOTP(): Dongle_info time reference set.";
	}
	
	/**
	 * Gets the current time of the dongle.
	 * 
	 * @return byte []  result
	 * 
	 * @author yassir
	 */
	public byte[] getTimeOTP() {
		String apdu = "D0B0000000";
		byte [] outData = null;
		try {
			DaplugApduCommand apduCommand = new DaplugApduCommand(
					DaplugUtils.hexStringToByteArray(apdu));
			DaplugApduResponse response = this.exchange(apduCommand);
			if(response.normalEnding()){
				if (response.getDataLen() != 0) {
					outData = new byte[response.getDataLen()];
					System.arraycopy(response.getData(), 0, outData, 0, response.getDataLen());
				}else{
					throw new Exception("getTimeOTP() : Dongle_info time reference not set yet !");
				}
			}else {
				throw new Exception( "getTimeOTP() - Cannot get dongle time !");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return outData;
	}
	
	/**
	 * Use the selected file as keyboard file. We must create a keyboard file then select it before using this function. (Refer to Keyborad functions
	 * to see how to create a keyboard file).
	 * @return String status value :
	 * 				if everything is ok : result = "useAsKeyboard(): Keyboard input file set"
	 * 				else result = "useAsKeyboard(): Cannot set keyboard input file !"
	 * @author yassir
	 */
	public String useAsKeyboard() {
		String apdu = "D032000000";
		boolean result = this.daplugMake(apdu);
		if (result == false)
			return "useAsKeyboard(): Cannot set keyboard input file !";
		else
			return "useAsKeyboard(): Keyboard input file set.";
	}
	
	/**
	 * Activates or deactivates keyboard emulation when the dongle is plugged.
	 * @param activated boolean A flag indicating if we want to enable/disable the keyboard emulation
	 * @return String result :
	 * 					if everything is ok : result = "setKeyboardAtBoot(): Automatic keyboard emulation activated."
	 * 					else result = "setKeyboardAtBoot(): Cannot activate automatic keyboard emulation !"
	 * @author yassir
	 */
	public String setKeyboardAtBoot(boolean activated) {
		StringBuilder apdu = new StringBuilder().append("D032");
		int active = (activated) ? 1 : 0; // convert boolean to int to used it
											// in switch
		switch (active) {
		case 1: // activated is true
			apdu.append("020000");
			boolean result1 = this.daplugMake(apdu.toString());
			if (result1 == false)
				return "setKeyboardAtBoot(): Cannot activate automatic keyboard emulation !";
			else
				return "setKeyboardAtBoot(): Automatic keyboard emulation activated.";
		case 0: // activated is false
			apdu.append("010000");
			boolean result0 = this.daplugMake(apdu.toString());
			if (result0 == false)
				return "setKeyboardAtBoot(): Cannot deactivate automatic keyboard emulation !";
			else
				return "SetKeyboardAtBoot(): Automatic keyboard emulation deactivated.";
		}
		return null;
	}
	
	/**
	 * Activates the virtual keyboard once, and plays the content associated as keyboard input file.
	 * @return String result :
	 * 				if everything is ok : result = "triggerKeyboard(): Keyboard triggered."
	 * 				else result = "triggerKeyboard(): CCannot trigger keyboard input !"
	 * @author yassir
	 */
	public String triggerKeyboard() {
		boolean result = this.daplugMake("D030010000");
		if (result == false)
			return "triggerKeyboard() : Cannot trigger keyboard input !";
		else
			return "triggerKeyboard() : Keyboard triggered.";
	}

	/**
	 * Change the dongle exchange mode from HID to WINUSB. Change will apply the next time the card boots (replugged or reset).
	 * @return  String result :
	 * 				if everything is ok : result = "hidToWinusb(): Winusb successfully activated!"
	 * 				else result = "hidToWinusb(): Can not switch dongle to winusb mode!"
	 * @author yassir
	 */
	public String hidToWinusb() {
		boolean result = this.daplugMake("D052080200");
		if (!result)
			return "hidToWinusb(): Can not switch dongle to winusb mode!";
		else
			return "hidToWinusb(): Winusb successfully activated!";
	}
	
	/**
	 * Change the dongle exchange mode from WINUSB to HID. Change will apply the next time the card boots (replugged or reset).
	 * @return  String result :
	 * 				if everything is ok : result = "winusbToHid(): HID successfully activated"
	 * 				else result = "winusbToHid(): Can not switch dongle to HID mode !"
	 * @author yassir
	 */
	public String winusbToHid() {
		boolean result = this.daplugMake("D052080100");
		if (result == false)
			return "winusbToHid(): Can not switch dongle to HID mode !";
		else
			return "winusbToHid(): HID successfully activated!";
	}
	
	/**
	 * Performs a warm reset of the dongle.
	 * @return  String result :
	 * 				if everything is ok : result = "reset(): Dongle successfully reset!"
	 * 				else result = "reset(): Can not reset dongle !"
	 * @author yassir
	 */
	public String reset() {
		boolean result = this.daplugMake("D052010000");
		if (result == false)
			return "reset(): Can not reset dongle !";
		else
			return "reset(): Dongle successfully reset!";
	}
	
	/**
	 * Blocks future commands sent to the dongle with a 6FAA status until the dongle is physically disconnected and reconnected from the USB port.
	 * @return  String result :
	 * 				if everything is ok : result = "halt(): Dongle successfully halted!"
	 * 				else result = "halt(): Can not halt dongle !"
	 * @author yassir
	 */
	public String halt() {

		boolean result = this.daplugMake("D052020000");
		if (result == false)
			return "halt(): Can not halt dongle !";
		else
			return "halt(): Dongle successfully halted!";
	}

    //=== private methods ===    
    
    //Wrap an Apdu command according to the current SC security level
    private DaplugApduCommand wrapApdu(DaplugApduCommand apdu){
    	    	
    	int		dataLen = apdu.getLc(),
    			macSize = 0,
        		padSize = 0,
        		orCla = 0x00;
  
    	byte[] 	header = new byte[DaplugApduCommand.APDU_HEADER_LEN],
    			finalHeader = new byte[DaplugApduCommand.APDU_HEADER_LEN],
    			data = new byte[dataLen],
    			tmpFinalData = new byte[DaplugApduCommand.APDU_DATA_MAX_LEN];
    	
    	header = apdu.getHeader();
    	data = apdu.getData();
    	System.arraycopy(header, 0, finalHeader, 0, DaplugApduCommand.APDU_HEADER_LEN);
   		System.arraycopy(data, 0, tmpFinalData, 0, dataLen);
    	
        //Data encryption (exclude external authenticate apdu : encryption will be applied for subsequent commands)
   		if(((this.securityLevel & SEC_LEVEL_C_DEC) != 0) && ((apdu.getCLA() != (0x80 & 0xFF) || (apdu.getINS() != (0x82 & 0xFF))))){
	    		
	    	//encrypt apdu data
	    	tmpFinalData  = DaplugCrypto.apduDataEncryption(data, this.sEncKey, DaplugCrypto.ENCRYPT);  
	    		
	        //padSize
	        int paddedDataLen = dataLen + 1; //add 0x80 to pad
	        while(paddedDataLen % 8 != 0){
	        	paddedDataLen++; //add 0x00 to pad
	        }
	        
	        padSize = paddedDataLen - dataLen;
	    }
    	
        //Command integrity (forced for external authenticate command)
        if(((this.securityLevel & SEC_LEVEL_C_MAC) != 0) || ((apdu.getCLA() == (0x80 & 0xFF) && (apdu.getINS() == (0x82 & 0xFF))))){
        	
        	byte[] tmpBuf = new byte[DaplugApduCommand.APDU_HEADER_LEN + dataLen];

            macSize = (byte)0x08;
            orCla = (byte)0x04;

            header[0]= (byte) (header[0] | orCla); //CLA ORed with 0x04 if c-mac
            header[4]= (byte) (header[4] + macSize); //increase Lc

  
            System.arraycopy(header, 0, tmpBuf, 0, DaplugApduCommand.APDU_HEADER_LEN);
           	System.arraycopy(data, 0, tmpBuf, DaplugApduCommand.APDU_HEADER_LEN, dataLen);
            
            //compute c-mac
            this.cMac = DaplugCrypto.computeRetailMac(tmpBuf, this.cMacKey, this.cMac, DaplugCrypto.C_MAC);
        }        
        
        //Final apdu header
        finalHeader[0]= (byte) (finalHeader[0] | orCla); //CLA ORed with 0x04 if c-mac
        finalHeader[4]= (byte)(finalHeader[4] + padSize + macSize); //increase Lc   
        
        //Final apdu data
        int finalDataLen = (finalHeader[4] - macSize) & 0xFF;
        byte[] finalData = new byte[finalDataLen];
        System.arraycopy(tmpFinalData, 0, finalData, 0, finalDataLen);

        //Final apdu
        int finalLc = finalHeader[4] & 0xFF;
        byte[] finalApduBuf = new byte[DaplugApduCommand.APDU_HEADER_LEN + finalLc];
        System.arraycopy(finalHeader, 0, finalApduBuf, 0, DaplugApduCommand.APDU_HEADER_LEN);
        System.arraycopy(finalData, 0, finalApduBuf, DaplugApduCommand.APDU_HEADER_LEN, finalDataLen);
        if(macSize == MAC_LEN){//if C_MAC
            System.arraycopy(this.cMac, 0, finalApduBuf, DaplugApduCommand.APDU_HEADER_LEN + finalDataLen, MAC_LEN);
        }    
        
        //Create DaplugApduCommand
    	DaplugApduCommand retApdu = null;
        try{
        	retApdu = new DaplugApduCommand(finalApduBuf);
        }catch(Exception e){
        	System.out.println(e.getMessage());
        }
        
    	return retApdu;
    }
    
    //Unwrap an Apdu response according to the current SC security level
    private DaplugApduResponse unwrapApdu (DaplugApduCommand apduCmd, byte[] apduRespBuf) throws Exception{
		
		//Original apdu response
		DaplugApduResponse tmpApduResp = null;
		try{
			tmpApduResp = new DaplugApduResponse(apduRespBuf);
		}catch(Exception e){
			System.err.println(e.getMessage());
		}
		
		byte[] 	data0 = null,
				paddedClearData = new byte[DaplugApduCommand.APDU_DATA_MAX_LEN],
				clearData = new byte[DaplugApduCommand.APDU_DATA_MAX_LEN],
				tempData = null,
				finalData = null,
				returnedMac = new byte[8],
				mac = new byte[8];
		
		//Sec level = r-mac
		if(((this.securityLevel & SEC_LEVEL_R_MAC) != 0) && ((this.securityLevel & SEC_LEVEL_R_ENC) == 0)){
			data0 = new byte[tmpApduResp.getDataLen() - 8];
			System.arraycopy(tmpApduResp.getData(), 0, data0, 0, tmpApduResp.getDataLen() - 8);
			System.arraycopy(tmpApduResp.getData(), tmpApduResp.getDataLen() - 8, returnedMac, 0, 8);
			
			//Compute the host r-mac and compare it with the returned mac (card r-mac)
			tempData = new byte[5+apduCmd.getLc()+data0.length+3];
			System.arraycopy(apduCmd.getBytes(), 0, tempData, 0, 5+apduCmd.getLc());
			byte[] tmp = DaplugUtils.hexStringToByteArray(String.format("%02X", data0.length));
			System.arraycopy(tmp, 0, tempData, 5+apduCmd.getLc(), 1);
			System.arraycopy(data0, 0, tempData, 5+apduCmd.getLc()+1, data0.length);
			System.arraycopy(tmpApduResp.getSW(), 0, tempData, 5+apduCmd.getLc()+1+data0.length, 2);
			
			mac = DaplugCrypto.computeRetailMac(tempData, this.rMacKey, this.rMac, DaplugCrypto.R_MAC);
			
			if(Arrays.equals(mac, returnedMac) == false){
				this.deAuthenticate();
				throw new Exception("Response integrity failed !");
			}
			
			System.arraycopy(mac, 0, this.rMac, 0, 8);
			finalData = new byte[tmpApduResp.getDataLen() - 8];
			System.arraycopy(data0, 0, finalData, 0, tmpApduResp.getDataLen() - 8);
			
		}
		
		//Sec level = r-enc
		if(((this.securityLevel & SEC_LEVEL_R_MAC) == 0) && ((this.securityLevel & SEC_LEVEL_R_ENC) != 0)){
			if(tmpApduResp.getDataLen()>0){
				byte[] encryptedData = new byte[tmpApduResp.getDataLen()];
				System.arraycopy(tmpApduResp.getData(), 0, encryptedData, 0, tmpApduResp.getDataLen());
				paddedClearData = DaplugCrypto.apduDataEncryption(encryptedData, this.rEncKey, DaplugCrypto.DECRYPT);
				//Exclude padding to obtain clear data
				int i  = paddedClearData.length - 1;
				while(paddedClearData[i] == 0 && i>0){
					i--;
				}
				if(paddedClearData[i] == (byte)0x80){
					System.arraycopy(paddedClearData, 0, clearData, 0, i);
				}else{
					this.deAuthenticate();
	    			throw new Exception("Response decryption failed !");	
				}
			
	    		finalData = new byte[i];
	    		System.arraycopy(clearData, 0, finalData, 0, i); 
			}    	
		}
		
		//Sec level = r-mac + r-enc
		if(((this.securityLevel & SEC_LEVEL_R_MAC) != 0) && ((this.securityLevel & SEC_LEVEL_R_ENC) != 0)){    		
			//encrypted data + r-mac
			data0 = new byte[tmpApduResp.getDataLen() - 8];
			System.arraycopy(tmpApduResp.getData(), 0, data0, 0, tmpApduResp.getDataLen() - 8);
			System.arraycopy(tmpApduResp.getData(), tmpApduResp.getDataLen() - 8, returnedMac, 0, 8);
			
			//decrypt data
			int i = 0;
			if(data0.length > 0){
				paddedClearData = DaplugCrypto.apduDataEncryption(data0, this.rEncKey, DaplugCrypto.DECRYPT);
				//Exclude padding to obtain clear data
				i  = paddedClearData.length - 1;
				while(paddedClearData[i] == 0 && i>0){
					i--;
				}
				if(paddedClearData[i] == (byte)0x80){
					System.arraycopy(paddedClearData, 0, clearData, 0, i);
				}else{
					this.deAuthenticate();
	    			throw new Exception("Response decryption failed !");	
				}
			}
			
			//Compute the host r-mac and compare it with the returned mac (card r-mac)
			tempData = new byte[5+apduCmd.getLc()+i+3];
			System.arraycopy(apduCmd.getBytes(), 0, tempData, 0, 5+apduCmd.getLc());
			byte[] tmp = DaplugUtils.hexStringToByteArray(String.format("%02X", i));
			System.arraycopy(tmp, 0, tempData, 5+apduCmd.getLc(), 1);
			System.arraycopy(clearData, 0, tempData, 5+apduCmd.getLc()+1, i);
			System.arraycopy(tmpApduResp.getSW(), 0, tempData, 5+apduCmd.getLc()+1+i, 2);
			
			mac = DaplugCrypto.computeRetailMac(tempData, this.rMacKey, this.rMac, DaplugCrypto.R_MAC);
			
			if(Arrays.equals(mac, returnedMac) == false){
				this.deAuthenticate();
				throw new Exception("Response integrity failed !");
			}
			
			System.arraycopy(mac, 0, this.rMac, 0, 8);
			finalData = new byte[i];
			System.arraycopy(clearData, 0, finalData, 0, i); 
			
		}
		
		//Sec level contains no r-mac or r-enc 
		if(((this.securityLevel & SEC_LEVEL_R_MAC) == 0) && ((this.securityLevel & SEC_LEVEL_R_ENC) == 0)){
			finalData = new byte[tmpApduResp.getDataLen()];
			System.arraycopy(tmpApduResp.getData(), 0, finalData, 0, tmpApduResp.getDataLen()); 
	   	}    	
		
		byte[] retResponseBytes = new byte[finalData.length + 2];  
		System.arraycopy(finalData, 0, retResponseBytes, 0, finalData.length);
		System.arraycopy(tmpApduResp.getSW(), 0, retResponseBytes, finalData.length, 2);
		
		DaplugApduResponse retResponse = null;
		try{
			retResponse = new DaplugApduResponse(retResponseBytes);
		}catch(Exception e){
			System.out.println(e.getMessage());
		}
		
		return retResponse;
	}
    
    //Encrypt/decrypt Apdu	
	private byte[] crypt(int keyVersion, int keyID, int mode, byte[] iv, byte[] div1, byte[] div2, byte[] inData, int enc) throws Exception{
    	
    	byte[] outData = null;
    	
    	int lc = 10; //kv, kid & iv
    	
    	//What function?
    	String functionName  = "", operationName = "";    	
    	if(enc == ENCRYPT){
    		functionName = "encrypt()";
    		operationName = "encryption";
    	} else if(enc == DECRYPT){
    		functionName = "decrypt()";
    		operationName = "decryption";
    	}else{
			throw new Exception(functionName + " - Invalid parameter : " + enc);
    	}
    	
    	//IV
    	if(iv == null){
    		iv = DaplugUtils.hexStringToByteArray("0000000000000000");
    	}
    	
    	//Check diversifiers validity
    	String div1_str = "", div2_str = "";
    	if((mode & ENC_1_DIV) != 0 || (mode & ENC_2_DIV) != 0){
	    	if(div1 != null){
	    		if(div1.length != 16){
	    			throw new Exception(functionName + " - Invalid diversifier length : " + DaplugUtils.byteArrayToHexString(div1));
	    		}else{
	    			div1_str = DaplugUtils.byteArrayToHexString(div1);
	    			lc = lc + 16;
	    		}
	    	}else{
    			throw new Exception(functionName + " - Diversifier div1 required !");
	    	}
    	}
    	if((mode & ENC_2_DIV) != 0){
	    	if(div2 != null){
	    		if(div2.length != 16){
	    			throw new Exception(functionName + " - Invalid diversifier length : " + DaplugUtils.byteArrayToHexString(div2));
	    		}else{
	    			div2_str = DaplugUtils.byteArrayToHexString(div2);
	    			lc = lc + 16;
	    		}
	    	}else{
    			throw new Exception(functionName + " - Diversifier div2 required !");
	    	}	    	
    	}
    	
    	//Check inData validity
    	if(inData.length % 8 != 0 || inData.length + lc > MAX_REAL_DATA_SIZE){
    		throw new Exception(functionName + " - Invalid data length !" );
    	}else{
    		lc = lc + inData.length;
    	}
    	
    	String hexStrBuf = "";
    	hexStrBuf = hexStrBuf.concat("D020");
    	hexStrBuf = hexStrBuf.concat(String.format("%02X", enc));
    	hexStrBuf = hexStrBuf.concat(String.format("%02X", mode));
    	hexStrBuf = hexStrBuf.concat(String.format("%02X", lc));
    	hexStrBuf = hexStrBuf.concat(String.format("%02X", keyVersion));
    	hexStrBuf = hexStrBuf.concat(String.format("%02X", keyID));
    	hexStrBuf = hexStrBuf.concat(DaplugUtils.byteArrayToHexString(iv));
    	hexStrBuf = hexStrBuf.concat(div1_str);
    	hexStrBuf = hexStrBuf.concat(div2_str);
    	hexStrBuf = hexStrBuf.concat(DaplugUtils.byteArrayToHexString(inData));
    	
    	try{
    		DaplugApduCommand a = new DaplugApduCommand(DaplugUtils.hexStringToByteArray(hexStrBuf));
    		DaplugApduResponse r = this.exchange(a);
    		if(r.normalEnding()){
    			outData = new byte[r.getDataLen()];
    			System.arraycopy(r.getData(), 0, outData, 0, r.getDataLen());
    		}else{
    			throw new Exception(functionName + " - Data " + operationName + " failed !");
    		}
    	}catch(Exception e){
    		System.err.println(e.getMessage());
    	}    	
    	
    	return outData;
    	
	}
	
//	private byte[] hmac_sha1(int keyVersion, int options, byte [] div1, byte [] div2, byte [] inData) throws Exception {
//		DaplugApduCommand hmac = null;
////		System.out.println("[hmac_sha1] param received :");
////		System.out.println("\t input data : " + DaplugUtils.byteArrayToHexString(inData));
////		System.out.println("\t mode       : " + options);
////		System.out.println("\t keyVersion : " + keyVersion);
//		
//		String hmac_apdu_str = "D022";
//		String s_keyVersion	 = String.format("%02X",keyVersion);
//		String s_options	 = String.format("%02X", options);
//		int lc = 0;
//		
//    	//Check diversifiers validity
//    	String div1_str = "", div2_str = "";
//    	if((options & OTP_1_DIV) != 0 || (options & OTP_2_DIV) != 0){
//	    	if(div1 != null){
//	    		if(div1.length != 16){
//	    			throw new Exception("hmac_sha1() - Invalid diversifier length : " + DaplugUtils.byteArrayToHexString(div1));
//	    		}else{
//	    			div1_str = DaplugUtils.byteArrayToHexString(div1);
//	    			lc = lc + 16;
//	    		}
//	    	}else{
//    			throw new Exception("hmac_sha1() - Diversifier div1 required !");
//	    	}
//    	}
//    	if((options & OTP_2_DIV) != 0){
//	    	if(div2 != null){
//	    		if(div2.length != 16){
//	    			throw new Exception("hmac_sha1() - Invalid diversifier length : " + DaplugUtils.byteArrayToHexString(div2));
//	    		}else{
//	    			div2_str = DaplugUtils.byteArrayToHexString(div2);
//	    			lc = lc + 16;
//	    		}
//	    	}else{
//    			throw new Exception("hmac_sha1() - Diversifier div2 required !");
//	    	}	    	
//    	} 
//		
//		String _data = DaplugUtils.byteArrayToHexString(inData);
//
//		//for Daplug_totp, data can be null => we exclude condition strlen(inData) = 0
//		if( (lc += (_data.length() / 2) )  > MAX_REAL_DATA_SIZE )
//			throw new Exception ("hmac_sha1() : Wrong length for input data : " + _data.length() );
//		
//		
//		
//		String lc_s = String.format("%02X", lc /*+ _data.length() / 2 */);
//		
//		System.out.println("lc   = " + lc + "\tlc_s = " + lc_s);
////		System.out.println("[hmac_sha1] format APDU : ");
////		System.out.println("\t hmac_apdu_str = " + hmac_apdu_str);
////		System.out.println("\t s_keyVersion  = " + s_keyVersion);
////		System.out.println("\t s_options     = " + s_options);
////		System.out.println("\t lc_s          = " + lc_s);
////		System.out.println("\t _div1         = " + _div1);
////		System.out.println("\t _div2         = " + _div2 );
////		System.out.println("\t _data[inData] = " + _data);
//		//System.out.println("--------------------------");
//		StringBuilder sb = new StringBuilder();
//		sb.append(hmac_apdu_str)
//			.append(s_keyVersion)
//			.append(s_options)
//			.append(lc_s)
//			.append(div1_str)
//			.append(div2_str)
//			.append(_data);
//		
////		System.out.println("[hmac_sha1] apdu = \n" + sb.toString());
////		System.out.println("--------------------------");
//		hmac = new DaplugApduCommand(DaplugUtils.hexStringToByteArray(sb.toString()));	
//		DaplugApduResponse result = exchange(hmac);	
//		if (!result.normalEnding())
//			throw new Exception("hmac_sha1() : Cannot sign data !");
//		
//		return result.getData();
//	}
	
	private byte[] hmacSha1(int keysetVersion, int options, byte[] div1, byte[] div2, byte[] inData, int sign) throws Exception{
		
		byte[] outData = null;
		int lc = 0;
    	
    	//What function?
    	String functionName  = "", operationName = "";    	
    	if(sign == HMAC){
    		functionName = "hmac()";
    		operationName = "hmac signature";
    	}else if(sign == HOTP){
    		functionName = "hotp()";
    		operationName = "hotp";
    	}else if(sign == TOTP){
    		functionName = "totp()";
    		operationName = "totp";
    	}else{
			throw new Exception(functionName + " - Invalid parameter : " + sign);
    	}
    	
    	//Check diversifiers validity
    	String div1_str = "", div2_str = "";
    	if((options & OTP_1_DIV) != 0 || (options & OTP_2_DIV) != 0){
	    	if(div1 != null){
	    		if(div1.length != 16){
	    			throw new Exception(functionName + " - Invalid diversifier length : " + DaplugUtils.byteArrayToHexString(div1));
	    		}else{
	    			div1_str = DaplugUtils.byteArrayToHexString(div1);
	    			lc = lc + 16;
	    		}
	    	}else{
    			throw new Exception(functionName + " - Diversifier div1 required !");
	    	}
    	}
    	if((options & OTP_2_DIV) != 0){
	    	if(div2 != null){
	    		if(div2.length != 16){
	    			throw new Exception(functionName + " - Invalid diversifier length : " + DaplugUtils.byteArrayToHexString(div2));
	    		}else{
	    			div2_str = DaplugUtils.byteArrayToHexString(div2);
	    			lc = lc + 16;
	    		}
	    	}else{
    			throw new Exception(functionName + " - Diversifier div2 required !");
	    	}	    	
    	}
    	
    	//Check inData validity
    	if (inData != null) {
	    	if(inData.length + lc > MAX_REAL_DATA_SIZE){
	    		throw new Exception(functionName + " - Invalid data length !" );
	    	}else{
	    		lc = lc + inData.length;
	    	}
    	} 
    	
    	String hexStrBuf = "";
    	hexStrBuf = hexStrBuf.concat("D022");
    	hexStrBuf = hexStrBuf.concat(String.format("%02X", keysetVersion));
    	hexStrBuf = hexStrBuf.concat(String.format("%02X", options));
    	hexStrBuf = hexStrBuf.concat(String.format("%02X", lc));
    	hexStrBuf = hexStrBuf.concat(div1_str);
    	hexStrBuf = hexStrBuf.concat(div2_str);
   		hexStrBuf = hexStrBuf.concat(DaplugUtils.byteArrayToHexString(inData));
   		
    	try{
    		DaplugApduCommand a = new DaplugApduCommand(DaplugUtils.hexStringToByteArray(hexStrBuf));
    		DaplugApduResponse r = this.exchange(a);
    		if(r.normalEnding()){
    			outData = new byte[r.getDataLen()];
    			System.arraycopy(r.getData(), 0, outData, 0, r.getDataLen());
    		}else{
    			throw new Exception(functionName + " - Generating " + operationName + " failed !");
    		}
    	}catch(Exception e){
    		System.err.println(e.getMessage());
    	}    	
    	
    	return outData;
		
	}
	
	private boolean daplugMake(String apdu) {
		try {
			DaplugApduCommand a = new DaplugApduCommand(
				DaplugUtils.hexStringToByteArray(apdu));
			DaplugApduResponse r = this.exchange(a);
			if (r.normalEnding())
				return true;
			else
				return false;
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return false;
	}
    
}