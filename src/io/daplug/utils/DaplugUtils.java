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

package io.daplug.utils;

public class DaplugUtils {

	/**
	 * @author Saada
	 * @param b
	 * @return
	 */
	public static String byteArrayToHexString(byte[] b) {
	
		if(b == null) return "";
		
		if(b.length == 0) return "";
			
		StringBuffer sb = new StringBuffer(b.length * 2);
		for (int i = 0; i < b.length; i++) {
			int v = b[i] & 0xff;
			if (v < 16) {
				sb.append('0');
			}
			sb.append(Integer.toHexString(v));
		}
		
		return sb.toString().toUpperCase();

	}

	/**
	 * convert an hexadecimal String to byte Array
	 * 
	 * @param s
	 *            String to convert
	 * @author Saada
	 * @return
	 */
	public static byte[] hexStringToByteArray(String s) {

		if (!isHexInput(s)) {
			System.err.println("Invalid hex string : " + s);
			return null; // or exception
		}
		
		if(s.length() == 0) return new byte[0];
		
		int len = s.length();
		byte[] b = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			b[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character
					.digit(s.charAt(i + 1), 16));
		}

		return b;
	}

	/**
	 * concat 2 array of byte in one
	 * 
	 * @param array1
	 *            byte[]
	 * @param array2
	 *            byte[]
	 * @author Saada
	 * @return
	 */
	public static byte[] byteArrayConcat(byte[] array1, byte[] array2) {
		byte[] result = new byte[array1.length + array2.length];
		System.arraycopy(array1, 0, result, 0, array1.length);
		System.arraycopy(array2, 0, result, array1.length, array2.length);
		return result;
	}

	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

	/**
	 * convert a byte array into hexadecimal String
	 * 
	 * @param bytes
	 *            byte[]array
	 * @return
	 * @author Yassir
	 */
	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	/**
	 * return an array of String with all split values
	 * 
	 * @param value
	 *            String value to split
	 * @param separator
	 *            String the separator
	 * @return String[]
	 * @author Yassir
	 */
	public static String[] splitString(String value, String separator) {
		if (value.contains(separator)) {
			String[] result = value.split(separator);
			return result;
		} else {
			throw new IllegalArgumentException("String " + value
					+ " does not contain separator : " + separator);
		}
	}

	/**
	 * 
	 * @param input String
	 * @return
	 * @author Saada
	 */
	public static boolean isHexInput(String input) {

		boolean is_hex = true;

		int i = 0, j;
		String hex_nb = "0123456789abcdefABCDEF";

		if (input.length() % 2 != 0) {
			return false;
		}

		while ((i < input.length()) && is_hex) {
			j = 0;
			while (j < 22) {
				if (input.charAt(i) != hex_nb.charAt(j)) {
					is_hex = false;
					j++;
				} else {
					is_hex = true;
					break;
				}
			}
			i++;
		}

		return is_hex;
	}

	/**
	 * convert int value to hexa
	 * @param n int value to convert
	 * @return int
	 * @author yassir
	 */
	public static int convertInttoHexa(int n) {
		  return Integer.valueOf(String.valueOf(n), 16);
		}
	
	
	/**
	 * convert ascii String into hexa String
	 * @param ascii String value to convert
	 * @return String
	 * @author yassir
	 */
	public static String asciiToHex(String ascii){
        StringBuilder hex = new StringBuilder();
        for (int i=0; i < ascii.length(); i++) {
        		hex.append(Integer.toHexString(ascii.charAt(i)));
        }
        return hex.toString();
    } 
	
	/**
	 * convert hexa string to ascii value
	 * @param hex String hexa to convert
	 * @return String 
	 * @author yassir
	 */
	public static String hexToASCII(String hex){       
        if(hex.length()%2 != 0){
           System.err.println("requires EVEN number of chars");
           return null;
        }
        StringBuilder sb = new StringBuilder();               
        //Convert Hex 0232343536AB into two characters stream.
        for( int i=0; i < hex.length()-1; i+=2 ){
            String output = hex.substring(i, (i + 2));
            int decimal = Integer.parseInt(output, 16);                 
            sb.append((char)decimal);             
        }           
        return sb.toString();
	} 
	
	/**
	 * convert a part of a byte array into a String. 
	 * @param array byte []
	 * @param byteLength int the length to consider for conversion
	 * @return result String
	 * @author yassir
	 */
	public static String byteToString(byte [] array, int byteLength) {
		return new String(array, 0, byteLength);
	}
}
