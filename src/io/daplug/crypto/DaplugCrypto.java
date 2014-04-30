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

package io.daplug.crypto;

import java.security.*;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import io.daplug.utils.*;

public class DaplugCrypto {
	
	public static final int C_MAC = 1,
					  		R_MAC = 0,
					  		ENCRYPT = 1,
					  		DECRYPT = 0;
	
	public static final byte[]	KEY_CONSTANT_S_ENC = {(byte)0x01,(byte)0x82},
					  			KEY_CONSTANT_R_ENC = {(byte)0x01,(byte)0x83},
					  			KEY_CONSTANT_C_MAC = {(byte)0x01,(byte)0x01},
					  			KEY_CONSTANT_R_MAC = {(byte)0x01,(byte)0x02},
					  			KEY_CONSTANT_DEK   = {(byte)0x01,(byte)0x81};
	
	//Obtain a 3DES key from a GP key
	private static byte[] GPKeyTo3DESKey(byte[] GPKey){
		
		byte[] _3DESKey = new byte[24];
		byte[] tmp = new byte[8];
		
		System.arraycopy(GPKey, 0, tmp, 0, 8);
		
		_3DESKey = DaplugUtils.byteArrayConcat(GPKey, tmp);
		
		return _3DESKey;
	}
		
	//Generate a challenge with a given size
	public static byte[] generateChallenge(int size){
		
		SecureRandom random = new SecureRandom();
		byte challenge[] = new byte[size];
		random.nextBytes(challenge);
		
		return challenge;
		
	}
	
	//Compute full 3DES mac
	public static byte[] computeFull3DesMac(byte[] data_buf, byte[] key_buf){
		
		KeySpec keySpec;
		SecretKey key; 
		IvParameterSpec iv; 
		
		byte[] temp = null;
		byte[] full3DesMac = new byte[8];
		byte[] pad = {(byte)0x80,0,0,0,0,0,0,0};
		byte[] iv_buf = {0,0,0,0,0,0,0,0};
		
		byte[] padded_data = DaplugUtils.byteArrayConcat(data_buf, pad);
		byte[] _3DESKey = GPKeyTo3DESKey(key_buf);			

		try{
			
			keySpec = new DESedeKeySpec(_3DESKey);
			key = SecretKeyFactory.getInstance("DESede").generateSecret(keySpec); 
		    iv = new IvParameterSpec(iv_buf); 
			
			Cipher cipher = Cipher.getInstance("DESede/CBC/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, key, iv);
			temp = cipher.doFinal(padded_data);
				
		}catch (Exception e){
			
			e.printStackTrace();
		}

		System.arraycopy(temp, temp.length - 8, full3DesMac, 0, 8);
		
		return full3DesMac;
		
	}
	
	//Compute the card cryptogram
	public static byte[] computeCardCryptogram(byte[] hostChallenge, byte[] cardChallenge, byte[] counter, byte[] s_encKey){
		
		byte[] cardCryptogram;
		
		byte[] temp = new byte[16];
		
		System.arraycopy(hostChallenge, 0, temp, 0, 8);
		System.arraycopy(counter, 0, temp, 8, 2);
		System.arraycopy(cardChallenge, 0, temp, 10, 6);
		
		cardCryptogram = computeFull3DesMac(temp, s_encKey);
			
		return cardCryptogram;
	}
	
	//Compute the host cryptogram
	public static byte[] computeHostCryptogram(byte[] hostChallenge, byte[] cardChallenge, byte[] counter, byte[] s_encKey){
		
		byte[] hostCryptogram;
		
		byte[] temp = new byte[16];
		
		System.arraycopy(counter, 0, temp, 0, 2);
		System.arraycopy(cardChallenge, 0, temp, 2, 6);
		System.arraycopy(hostChallenge, 0, temp, 8, 8);
		
		hostCryptogram = computeFull3DesMac(temp, s_encKey);
			
		return hostCryptogram;
	}
	
	//Compute a session key
	public static byte[] computeSessionKey(byte[] counter, byte[] keyConstant, byte[] masterKey){
		
		byte[] sessionKey = new byte[16];
		
		KeySpec keySpec;
		SecretKey key; 
		IvParameterSpec iv; 
		
		byte[] temp = new byte[16];
		byte[] pad = DaplugUtils.hexStringToByteArray("00000000000000000000");
		byte[] iv_buf = DaplugUtils.hexStringToByteArray("0000000000000000");
		
		System.arraycopy(keyConstant, 0, temp, 0, 2);
		System.arraycopy(counter, 0, temp, 2, 2);
		System.arraycopy(pad, 0, temp, 4, 10);
		
		byte[] _3DESKey = GPKeyTo3DESKey(masterKey);
		
		try{
			
			keySpec = new DESedeKeySpec(_3DESKey);
			key = SecretKeyFactory.getInstance("DESede").generateSecret(keySpec); 
		    iv = new IvParameterSpec(iv_buf); 
			
			Cipher cipher = Cipher.getInstance("DESede/CBC/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, key, iv);
			sessionKey = cipher.doFinal(temp);
				
		}catch (Exception e){
			
			e.printStackTrace();
		}
		
		return sessionKey;
	}
	
	//Check card cryptogram
	public static boolean checkCardCryptogram(byte[] returnedCardCryptogram, byte[] computedCardCryptogram){
		
		return Arrays.equals(returnedCardCryptogram, computedCardCryptogram);
	}
	
	//Compute the retail mac
	public static byte[] computeRetailMac(byte[] data, byte[] m_key, byte[] previousMac, int mac){
		
		int len = 0;
		byte[] temp, work1, work2, work3, work4;
		byte[] retailMac = new byte[8];
		byte[] icv = new byte[8];
		
		KeySpec keySpec;
		SecretKey key; 
		IvParameterSpec iv; 
		
		//icv
		if(mac == C_MAC){ //if c-mac case => zero IV 
			icv = DaplugUtils.hexStringToByteArray("0000000000000000");
		}
		if(mac == R_MAC){ //r-mac case
			System.arraycopy(previousMac, 0, icv, 0, previousMac.length);
		}
		
		temp = new byte[1024];
		if(mac == C_MAC){ //c-mac case
			System.arraycopy(previousMac, 0, temp, 0, previousMac.length);
			System.arraycopy(data, 0, temp, previousMac.length, data.length);
			//padding
			temp[previousMac.length+data.length] = (byte)0x80;
			len = previousMac.length + data.length + 1;
			int j = 0;
			while(len % 8 != 0){
				temp[previousMac.length+data.length+1+j] = 0x00;
				len++;
				j++;
			}
		}
		
		if(mac == R_MAC){ //r-mac case
			System.arraycopy(data, 0, temp, 0, data.length);
			//padding
			temp[data.length] = (byte)0x80;
			len = data.length + 1;
			int j = 0;
			while(len % 8 != 0){
				temp[data.length+1+j] = 0x00;
				len++;
				j++;
			}
		}
		
		//*Simple DES-CBC using the first part of the key on L-8 temp bytes
		byte[] key1 = new byte[8];
		System.arraycopy(m_key, 0, key1, 0, 8);
		work1 = new byte[len - 8];
		work4 = new byte[len - 8];
		System.arraycopy(temp, 0, work1, 0, len-8);	

		try{
		    key = new SecretKeySpec(key1, 0, key1.length, "DES");
		    iv = new IvParameterSpec(icv); 
			
			Cipher cipher = Cipher.getInstance("DES/CBC/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, key, iv);
			work4 = cipher.doFinal(work1);
		}catch (Exception e){			
			e.printStackTrace();
		}
		//*/
		
		work2 = new byte[8]; //last 8 bytes of temp
		System.arraycopy(temp, len-8, work2, 0, 8);
		
		//*Exclusive OR between last 8 bytes of temp and the last block of the last simple DES
		work3 = new byte[8];
		for(int i=0; i<8;i++){
			work3[i] = (byte) (work2[i] ^ work4[i+len-8-8]);
		}
		//*/
		
		//*Triple DES-ECB on the last result
		byte[] _3DESKey = GPKeyTo3DESKey(m_key);
		try{
			keySpec = new DESedeKeySpec(_3DESKey);
			key = SecretKeyFactory.getInstance("DESede").generateSecret(keySpec); 
			
			Cipher cipher = Cipher.getInstance("DESede/ECB/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, key);
			retailMac = cipher.doFinal(work3);
				
		}catch (Exception e){			
			e.printStackTrace();
		}
		
		return retailMac;
	}
	
	//Compute key check value
	public static byte[] computeKCV(byte[] key){
		
		byte[] temp = new byte[8], buf, kcv = new byte[3];
		KeySpec keySpec;
		SecretKey k;
		
		buf = DaplugUtils.hexStringToByteArray("0000000000000000");
		byte[] _3DESKey = GPKeyTo3DESKey(key);
		try{
			keySpec = new DESedeKeySpec(_3DESKey);
			k = SecretKeyFactory.getInstance("DESede").generateSecret(keySpec); 
			
			Cipher cipher = Cipher.getInstance("DESede/ECB/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, k);
			temp = cipher.doFinal(buf);
				
		}catch (Exception e){			
			e.printStackTrace();
		}
		
		System.arraycopy(temp, 0, kcv, 0, 3);
		
		return kcv;
		
	}
	
	//Compute diversified key value
	public static byte[] computeDiversifiedKey(byte[] key, byte[] diversifier){
		
		byte[] divkey = new byte[16];
		
		KeySpec keySpec;
		SecretKey k;
		IvParameterSpec iv; 
		
		byte[] _3DESKey = GPKeyTo3DESKey(key);
		try{
			keySpec = new DESedeKeySpec(_3DESKey);
			k = SecretKeyFactory.getInstance("DESede").generateSecret(keySpec);
		    iv = new IvParameterSpec(DaplugUtils.hexStringToByteArray("0000000000000000")); 
			
			Cipher cipher = Cipher.getInstance("DESede/CBC/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, k, iv);
			divkey = cipher.doFinal(diversifier);
				
		}catch (Exception e){			
			e.printStackTrace();
		}
		
		return divkey;
	}
	
	public static byte[] apduDataEncryption(byte[] data, byte[] key, int encrypt){
		
		byte[] result = new byte[255];
		byte[] temp = new byte[255];
		byte[] padded_data = null;
		int len = 0;
		
		System.arraycopy(data, 0, temp, 0, data.length);
		//padding if encryption case
		if(encrypt != DECRYPT){
			temp[data.length] = (byte) 0x80;
			int i = 0;
			len = data.length + 1;
			while(len % 8 != 0){
				temp[i + len] = 0x00;
				len++;
				i++;
			}			
			padded_data = new byte[len];
			System.arraycopy(temp, 0, padded_data, 0, len);
		}
		
		//encrypted padded data
		if(encrypt == DECRYPT){
			padded_data = new byte[data.length];
			System.arraycopy(temp, 0, padded_data, 0, data.length);
		}
		
		KeySpec keySpec;
		SecretKey k;
		IvParameterSpec iv; 
		
		byte[] _3DESKey = GPKeyTo3DESKey(key);
		try{
			keySpec = new DESedeKeySpec(_3DESKey);
			k = SecretKeyFactory.getInstance("DESede").generateSecret(keySpec);
		    iv = new IvParameterSpec(DaplugUtils.hexStringToByteArray("0000000000000000")); 
			
			Cipher cipher = Cipher.getInstance("DESede/CBC/NoPadding");
			if(encrypt != DECRYPT){
				cipher.init(Cipher.ENCRYPT_MODE, k, iv);
			}
			if(encrypt == DECRYPT){
				cipher.init(Cipher.DECRYPT_MODE, k, iv);
			}			
			result = cipher.doFinal(padded_data);
				
		}catch (Exception e){			
			e.printStackTrace();
		}
		
		return result;
		
	}	
	
	public static byte[] tripleDES_CBC_GP(byte[] data, byte[] key, byte[] iv, int enc) throws Exception{
		
		byte[] newData = null;
		KeySpec keySpec;
		SecretKey k;
		IvParameterSpec icv; 
		
		if(key.length != 16){
			throw new Exception("tripleDES_CBC_GP() - Invalid GP key : " + DaplugUtils.byteArrayToHexString(key));
		}
		
		if(data.length % 8 != 0){
			throw new Exception("tripleDES_CBC_GP() - Invalid data length !");			
		}
		
		if(iv == null){
			iv = DaplugUtils.hexStringToByteArray("0000000000000000");
		}
		
		if(iv.length != 8){
			throw new Exception("tripleDES_CBC_GP() - Invalid IV : " + DaplugUtils.byteArrayToHexString(iv));			
		}
		
		byte[] _3DESKey = GPKeyTo3DESKey(key);
		
		try{
			keySpec = new DESedeKeySpec(_3DESKey);
			k = SecretKeyFactory.getInstance("DESede").generateSecret(keySpec); 
		    icv = new IvParameterSpec(iv); 

			Cipher cipher = Cipher.getInstance("DESede/CBC/NoPadding");
			if(enc != DECRYPT){
				cipher.init(Cipher.ENCRYPT_MODE, k, icv);
			}
			if(enc == DECRYPT){
				cipher.init(Cipher.DECRYPT_MODE, k, icv);
			}						
			newData = cipher.doFinal(data);				
		}catch (Exception e){			
			e.printStackTrace();
		}
		
		return newData;
		
	}

	public static byte[] tripleDES_ECB_GP(byte[] data, byte[] key, int enc) throws Exception{
		
		byte[] newData = null;
		KeySpec keySpec;
		SecretKey k;
		
		if(key.length != 16){
			throw new Exception("tripleDES_ECB_GP() - Invalid GP key : " + DaplugUtils.byteArrayToHexString(key));
		}
		
		if(data.length % 8 != 0){
			throw new Exception("tripleDES_ECB_GP() - Invalid data length !");			
		}
		
		byte[] _3DESKey = GPKeyTo3DESKey(key);
		
		try{
			keySpec = new DESedeKeySpec(_3DESKey);
			k = SecretKeyFactory.getInstance("DESede").generateSecret(keySpec); 
			Cipher cipher = Cipher.getInstance("DESede/ECB/NoPadding");
			if(enc != DECRYPT){
				cipher.init(Cipher.ENCRYPT_MODE, k);
			}
			if(enc == DECRYPT){
				cipher.init(Cipher.DECRYPT_MODE, k);
			}						
			newData = cipher.doFinal(data);				
		}catch (Exception e){			
			e.printStackTrace();
		}
		
		return newData;
		
	}
	
}


