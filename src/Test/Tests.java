
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
*   Yassir Houssen Aabdullah <a.yassirhoussem@plug-up.com>
*/

package Test;

import io.daplug.keyboard.DaplugKeyboard;
import io.daplug.keyset.DaplugKeyset;
import io.daplug.session.DaplugSession;
import io.daplug.utils.DaplugUtils;

public class Tests {
	
	private static int	counterFileId = 0xc01d;
	private static int 	modhexFileId = 0x0001;
	private static int 	keyboardFileId = 0x0800;
	private static int 	hmacKeySize = 48;
	private static int 	transientExportKeyVersion = 0xFD;
	
	private static String counterFileId_str = "c01d";
	
	//Session
	private static DaplugSession ds = new DaplugSession();
	
	//*** Keysets ***//
	
	//adminKeyset (0x01) already exists on the card. It acts as a specific administrative code with extended capabilities for dongle management.
	private static DaplugKeyset adminKeyset = new DaplugKeyset(0x01, DaplugUtils.hexStringToByteArray("404142434445464748494a4b4c4d4e4f"));
	
	//Diversified adminKeyset : Use diversified adminKeyset keys
	private static DaplugKeyset divAdminKeyset;
	
	/* 	The transientKeyset (0xF0) is a virtual keyset located in RAM.. wich can be exported & imported.
    	When exported, the keyset is encrypted with a transient export keyset (role 0x0F)
    	In our test we use the ENC key of the existing transient export keyset (0xFD)
    	
    	access first byte codes the key access (here : 0 = always), access second byte codes the minimum security level mask required to open a Secure Channel using this keyset
	*/
	private static DaplugKeyset transientKeyset = new DaplugKeyset(0xF0, DaplugKeyset.USAGE_GP, DaplugSession.SEC_LEVEL_C_MAC, DaplugUtils.hexStringToByteArray("0123456789abcdef0123456789abcdef"));
	
	//anyKeyset, used just for testing new keyset upload
	//access first byte codes the key access (here : 0 = always), access second byte codes the minimum security level mask required to open a Secure Channel using this keyset
	private static DaplugKeyset anyKeyset = new DaplugKeyset(0x55, DaplugKeyset.USAGE_GP, DaplugSession.SEC_LEVEL_C_MAC, DaplugUtils.hexStringToByteArray("202122232425262728292a2b2c2d2e2f"));
	
	//Encryption/decryption keyset, access first byte codes the key access (here : 0 = always), access second byte codes the decryption access (here 0 = always)
	private static DaplugKeyset encDecKeyset = new DaplugKeyset((byte)0x56, DaplugKeyset.USAGE_ENC_DEC, 0, DaplugUtils.hexStringToByteArray("0123456789abcdef0123456789abcdef"));
	
	//Hmac-SHA1 keyset, access first byte codes the key access (here : 0 = always), access second byte codes the key length (must be < 48)
	private static int hmacKeysetAccess = (0 << 8) + hmacKeySize;
	private static DaplugKeyset hmacKeyset = new DaplugKeyset((byte)0x57, DaplugKeyset.USAGE_HMAC_SHA1, hmacKeysetAccess, DaplugUtils.hexStringToByteArray("0123456789abcdef0123456789abcdef"));
	
	//Hotp keyset, access first byte codes the key access (here : 0 = always), access second byte codes the key length (must be < 48)
	private static int hotpKeysetAccess = (0 << 8) + hmacKeySize;
	private static DaplugKeyset hotpKeyset = new DaplugKeyset(0x58, DaplugKeyset.USAGE_HOTP, hotpKeysetAccess, DaplugUtils.hexStringToByteArray("0123456789abcdef0123456789abcdef"));
	
	//Time source keyset, access first byte codes the key access (here : 0 = always), access second byte is not meaningful here
	private static DaplugKeyset timeSrcKeyset = new DaplugKeyset((byte)0x59, DaplugKeyset.USAGE_TOTP_TIME_SRC, 0, DaplugUtils.hexStringToByteArray("0123456789abcdef0123456789abcdef"));
	
	//Totp keyset, access first byte codes the time source keyset version, access second byte codes the key length (must be < 48)
	private static int totpKeysetAccess = (timeSrcKeyset.getVersion() << 8) + hmacKeySize;
	private static DaplugKeyset totpKeyset = new DaplugKeyset((byte)0x60, DaplugKeyset.USAGE_TOTP, totpKeysetAccess, DaplugUtils.hexStringToByteArray("0123456789abcdef0123456789abcdef"));
	
	
	//Diversifiers
	private static byte[]	diversifier1 = DaplugUtils.hexStringToByteArray("0123456789abcdeffedcba9876543210"),
							diversifier2 = DaplugUtils.hexStringToByteArray("fedcba98765432100123456789abcdef");
	

	/**
	 * @author Saada
	 */
	public static void testDongleDetection(){
		try{
			ds.getDonglesList();
			ds.getFirstDongle();
			System.out.println("First connected dongle selected...");
		}catch(Exception e){
			System.err.println(e.getMessage());
		}
	}
	
	/**
	 * 
	 * @param level combination of possible values SEC_LEVEL_C_MAC, SEC_LEVEL_C_DEC, SEC_LEVEL_R_MAC, & SEC_LEVEL_R_ENC.
     * SEC_LEVEL_C_MAC is forced
	 * @author Saada
	 */
	public static void testAuthentication(int level, boolean diversifiedAuth){
		
        try{
        	if(diversifiedAuth){
        		divAdminKeyset = ds.computeDiversifiedKeys(adminKeyset, diversifier1);
            	ds.authenticate(divAdminKeyset, level, diversifier1, null);
        	}else{
        		ds.authenticate(adminKeyset, level);        		
        	}
		}catch(Exception e){
			System.err.println(e.getMessage());
		}
	}
	
	/**
	 * 
	 * @param mode
	 * @author Saada
	 */
	public static void testModeSwitching(int mode){
		
        try{
        	if(mode == DaplugSession.HID_DEVICE){
        		ds.winusbToHid();
        	}else if(mode == DaplugSession.WINUSB_DEVICE){
        		ds.hidToWinusb();
        	}else{
        		throw new IllegalArgumentException("testModeSwitching() - Invalid mode : " + mode);
        	}
       		}catch(Exception e){
			System.err.println(e.getMessage());
		}
		
	}
	
	/**
	 * @author Saada
	 */
	public static void testGetSerial(){
		
        try{
			System.out.println("SERIAL = " + DaplugUtils.byteArrayToHexString(ds.getDongleSerial()));
		}catch(Exception e){
			System.err.println(e.getMessage());
		}
		
	}
	
	/**
	 * @author Saada
	 */
	public static void testGetStatus(){
		
        try{
			System.out.println("STATUS = " + ds.getDongleStatus());
		}catch(Exception e){
			System.err.println(e.getMessage());
		}
		
	}
	
	/**
	 * @author Saada
	 */
	public static void testPutkey(){
		
        try{
			System.out.println("Creating any new keyset...");
			ds.putKey(anyKeyset, false);
			System.out.println("Try authentication on the new created keyset...");
			ds.authenticate(anyKeyset, 8);
			//Clean card
			System.out.println("Cleaning card : deleting the new created keyset...");
			ds.authenticate(adminKeyset, 0);
			ds.deleteKey(anyKeyset.getVersion());
		}catch(Exception e){
			System.err.println(e.getMessage());
		}
		
	}
	
	/**
	 * @author Saada
	 */
	public static void testExportKey(){
		
        try{
			System.out.println("Create and export the transient keyset...");
			ds.putKey(transientKeyset, false);
			System.out.println("EXPORTED KEY BLOB = " + DaplugUtils.byteArrayToHexString(ds.exportKey(transientExportKeyVersion, 1)));
		}catch(Exception e){
			System.err.println(e.getMessage());
		}
		
	}
	
	/**
	 * @author Saada
	 */
	public static void testImportKey(byte[] blob){
		
        try{
			System.out.println("Import transient keyset...");
			ds.importKey(transientExportKeyVersion, 1, blob);
			System.out.println("Try authentication on the imported keyset...");
			ds.authenticate(transientKeyset, 8);
		}catch(Exception e){
			System.err.println(e.getMessage());
		}
		
	}
	
	/**
	 * @author Saada
	 */
	public static void testFS(){
		
        try{	
			int[] access = {0, 0, 0};
			ds.selectFile(DaplugSession.FS_MASTER_FILE);
			ds.createDir(100, access);
			ds.createFile(1001, 250, access, false, false);
			ds.selectFile(1001);
			String data = "722AA358F09948D3179C362D49A6D2D9FAB4B10A52AFE52A9C1613CFF23A69926A314489DCB8001BF90D813CB31C1E522B7BBDF43E5220B2A4C0D43281608DF327B8D36EEC7A6718BD60B4A4603973627163CC0524DCF80E3B3A0051F06366845D2AFDAC89DDCD788F6A7E278AFC86E167A085417A7025986F4B539B21CD149EA0F264DA240285F17F56B539B265CBD5BA48E02B9CDC58BB916B1AB58ACB350C7BB708E111262A6687838D1D1DE7546C340DF8A35A096B13E26DCBBA9B800A6FFF000A208B4B3F4F0373DDB1170ED0D1671C21FB6921217D4D82F7A19ECCD8981D33819D6CB781C496608CB1218518000102";
			ds.writeData(0, DaplugUtils.hexStringToByteArray(data));
			System.out.println("Write data = " + data);
			System.out.println("Read data = " + DaplugUtils.byteArrayToHexString(ds.readData(0, 242)));
			ds.selectFile(DaplugSession.FS_MASTER_FILE);
			ds.deleteFileOrDir(100);
		}catch(Exception e){
			System.err.println(e.getMessage());
		}
	}
	
	/**
	 * @author Saada
	 */
	public static void testEncDec(){
		
        try{
			ds.putKey(encDecKeyset, false);
			byte[] inData = DaplugUtils.hexStringToByteArray("0123456789abcdef0123456789abcdef");
			byte[] outData = new byte[inData.length];
			byte[] div1 = DaplugUtils.hexStringToByteArray("0123456789abcdef0123456789abcdef");
			byte[] div2 = DaplugUtils.hexStringToByteArray("0123456789abcdef0123456789abcdef");
			int options = DaplugSession.ENC_CBC+DaplugSession.ENC_2_DIV; //use cbc mode & two diversifiers
			System.out.println("Clear Data = " + DaplugUtils.byteArrayToHexString(inData));
			outData = ds.encrypt(encDecKeyset.getVersion(), 1, options, null, div1, div2, inData);
			System.out.println("Encrypted Data = " + DaplugUtils.byteArrayToHexString(outData));
			outData = ds.decrypt(encDecKeyset.getVersion(), 1, options, null, div1, div2, outData);
			System.out.println("Decrypted Data = " + DaplugUtils.byteArrayToHexString(outData));
			ds.deleteKey(encDecKeyset.getVersion());
		}catch(Exception e){
			System.err.println(e.getMessage());
		}
	}
	
	/**
	 * 
	 * @param size Random length
	 * @author Yassir
	 */
	public static void testGenerateRandom(int size){
        try{
			System.out.println(size + "-bytes RANDOM = " + DaplugUtils.byteArrayToHexString(ds.getRandom(size)));
		}catch(Exception e){
			System.err.println(e.getMessage());
		}
	}
	
	/**
	 * @author Yassir
	 */
	public static void testHmac(){
        try{
        	
        	int options = DaplugSession.OTP_2_DIV; //or DaplugSession.OTP_0_DIV or DaplugSession.OTP_1_DIV
        	
        	ds.putKey(hmacKeyset, false);
        	byte[] anyData = DaplugUtils.hexStringToByteArray("012548deac475c5e478fde001111111144dddddddfea09999999999995");
        	byte[] signature = ds.hmac(hmacKeyset.getVersion(), options, diversifier1, diversifier2, anyData);
        	
        	System.out.println("Data to sign = " + DaplugUtils.byteArrayToHexString(anyData));
        	System.out.println("Signature = " + DaplugUtils.byteArrayToHexString(signature));
        	
        	//Clean Card
        	System.out.println("Clean card...");
        	ds.deleteKey(hmacKeyset.getVersion());
        	
		}catch(Exception e){
			System.err.println(e.getMessage());
		}
	}
	
	/**
	 * @author Yassir
	 */
	public static void testHotp(){
        try{
        	
        	/*
        		{DaplugSession.OTP_0_DIV or DaplugSession.OTP_1_DIV}
        		+
        		{DaplugSession.OTP_6_DIGIT or DaplugSession.OTP_7_DIGIT or DaplugSession.OTP_8_DIGIT}	
        	*/
        	int options = DaplugSession.OTP_2_DIV + DaplugSession.OTP_7_DIGIT; 
			int [] access = {DaplugSession.ACCESS_ALWAYS, DaplugSession.ACCESS_ALWAYS, DaplugSession.ACCESS_ALWAYS};
        	
        	ds.putKey(hotpKeyset, false);
	        //Try to create counter file
			System.out.println("Try to create counter file...");
			ds.selectPath("3f00:c010");
			ds.createFile(counterFileId, 8, access, false, true);
        	
			byte[] hotp = ds.hotp(hotpKeyset.getVersion(), options, diversifier1, diversifier2, 
        			DaplugUtils.hexStringToByteArray(counterFileId_str));
        	
        	System.out.println("HOTP = " + DaplugUtils.byteArrayToHexString(hotp));
        	
        	//Clean Card
        	System.out.println("Clean card...");
		    //try to remove counter file
			ds.selectPath("3f00:c010");
			ds.deleteFileOrDir(counterFileId);
		    //try to remove the hotp keyset
        	ds.deleteKey(hotpKeyset.getVersion());
        	
		}catch(Exception e){
			System.err.println(e.getMessage());
		}
	}
	
	/**
	 * @author Yassir
	 */
	public static void testTotp(){
        try{
        	
        	/*
        		{DaplugSession.OTP_0_DIV or DaplugSession.OTP_1_DIV}
        		+
        		{DaplugSession.OTP_6_DIGIT or DaplugSession.OTP_7_DIGIT or DaplugSession.OTP_8_DIGIT}	
        	*/
        	int options = DaplugSession.OTP_2_DIV + DaplugSession.OTP_7_DIGIT; 
        	
			ds.putKey(timeSrcKeyset, false);
        	ds.putKey(totpKeyset, false);
        	
        	int keyId = 0;
        	ds.setTimeOTP(timeSrcKeyset.getVersion(), keyId+1, timeSrcKeyset.getKey(keyId));
        	byte[] totp = ds.totp(totpKeyset.getVersion(), options, diversifier1, diversifier2, null);
        	
        	System.out.println("TOTP = " + DaplugUtils.byteArrayToHexString(totp));
        	
        	//Clean Card
        	System.out.println("Clean card...");
		    //try to remove the totp keyset & time source keyset
        	ds.deleteKey(totpKeyset.getVersion());
        	ds.deleteKey(timeSrcKeyset.getVersion());
        	
		}catch(Exception e){
			System.err.println(e.getMessage());
		}
	}
	
	/**
	 * @author Saada
	 */
	public static void testKeyboard(String url, boolean makeHotp, int hotpFormat, byte[] divForHotp) {
		
		try {

			int [] access = {DaplugSession.ACCESS_ALWAYS, DaplugSession.ACCESS_ALWAYS, DaplugSession.ACCESS_ALWAYS};
			
			int options = hotpFormat;//Digits or modhex
			
			//Use Diversifier?
			if(divForHotp != null){
				//Diversifier validity
				if(divForHotp.length != 16){
					throw new Exception("testKeyboard() - Invalid diversifier : " + DaplugUtils.byteArrayToHexString(divForHotp));
				}else{
					options = options + DaplugKeyboard.HOTP_USE_DIV;
				}
			}
			
			//Make Hotp
			if(makeHotp){
				
				if((options & DaplugKeyboard.HOTP_MODHEX) != 0){
		            //When using modhex output for hotp, try to create file "3f00/0001"
					System.out.println("Try to create modhex mapping file...");
					ds.selectPath("3f00");
					ds.createFile(modhexFileId, 16, access, false, false);
					ds.selectFile(modhexFileId);
		            //write Hid Code used  for mapping (refer to product specification for more details - section "keyboard file")
					ds.writeData(0, DaplugUtils.hexStringToByteArray("06050708090a0b0c0d0e0f1115171819"));
			        //try to create Hotp keyset
					System.out.println("Try to create Hotp keyset...");
					ds.putKey(hotpKeyset, false);
			        //Try to create counter file
					System.out.println("Try to create counter file...");
					ds.selectPath("3f00:c010");
					ds.createFile(counterFileId, 8, access, false, true);
				}
				
			}			

			int filesize = 500;			
			System.out.println("Try to create keyboard file...");
			ds.selectFile(DaplugSession.FS_MASTER_FILE);
			ds.createFile(keyboardFileId, filesize, access, false, false);
			ds.selectFile(keyboardFileId);
			
			System.out.println("Setting keyboard file content...");			
			DaplugKeyboard kb = new DaplugKeyboard();
			//detect if host is win or mac
			kb.addOSProbe(-1, -1, -1);
			
			//windows version
			kb.addIfPC();
			//send win + R without sending bunk stuff before
			kb.addOSProbeWinR(-1, 0xF000, -1);
			//wait a bit for command windows to appear
			kb.addSleep(-1);
			//Type in the link address
			kb.addTextWindows(url);			
			if(makeHotp){
				//Add Hotp code
				kb.addHotpCode(options,0x08,hotpKeyset.getVersion(),counterFileId,DaplugUtils.byteArrayToHexString(divForHotp));
			}
			//Add return
			kb.addReturn();
			//mac version
			kb.addIfMac();
			//Type cmd + space, then release key
		    kb.addKeyCodeRelease("01082c");
		    //wait a bit for spotlight to appear
		    kb.addSleep(0x14000);
		    //Type "Safari<wait><return>"
		    kb.addTextMac("Safari.app",0,-1);
		    kb.addSleep(0x3c000);
		    kb.addReturn();
		    kb.addSleep(-1);
		    kb.addSleep(-1);
		    //if azerty: erase and retry
		    kb.addKeyCodeRaw("2A2A2A2A2A2A2A2A2A2A");//backspace
		    kb.addTextMac("Safari.app",1,-1);
		    kb.addSleep(0x3c000);
		    kb.addReturn();
		    //wait for Safari to appear (and possibly load the default page)
		    kb.addSleep(0x78000);
		    //select new tab cmd + T
		    kb.addKeyCodeRelease("010817");
		    kb.addSleep(0x78000);
		    //Get back the focus just in case with cmd+L
		    kb.addKeyCodeRelease("01080f");
		    kb.addSleep(0x3c000);
		    //Type the url (qwerty)
		    kb.addTextMac(url,0,-1);
		    //add hotp code and return
			if(makeHotp){
				//Add Hotp code
				kb.addHotpCode(options,0x08,hotpKeyset.getVersion(),counterFileId,DaplugUtils.byteArrayToHexString(divForHotp));
			}
			//Add return
			kb.addReturn();
		    //wait for the page to load
		    kb.addSleep(0x14000);
		    //cmd + w close tab with the opposite layout
		    kb.addKeyCodeRelease("01081d");
		    //Then retry with the other keyset
		    //selectnew tab cmd+T
		    kb.addKeyCodeRelease("010817");
		    kb.addSleep(0x78000);
		    //Get back the focus just in case with cmd+L
		    kb.addKeyCodeRelease("01080f");
		    kb.addSleep(0x3c000);
		    //Type the url (azerty)
		    kb.addTextMac(url,1,-1);
			if(makeHotp){
				//Add Hotp code
				kb.addHotpCode(options,0x08,hotpKeyset.getVersion(),counterFileId,DaplugUtils.byteArrayToHexString(divForHotp));
			}
			//Add return
			kb.addReturn();
		    //wait for the page to load
		    kb.addSleep(0x14000);
		    //cmd + w close tab with the opposite layout
		    kb.addKeyCodeRelease("01081a");
			
		    //ensure zeroized to avoid misinterpretaion
		    kb.zeroPad(filesize);
		    
		    ds.writeData(0, DaplugUtils.hexStringToByteArray(kb.getContent()));
		    
		    System.out.println("Setting current file as keyboard file...");
		    ds.useAsKeyboard();
		    
		    System.out.println("Activating keyboard boot...");
		    ds.setKeyboardAtBoot(true);

		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @author Saada
	 */
	public static void testDisableKeyboard(){
		
		try{
			System.out.println("Disable keyboard...");
			ds.setKeyboardAtBoot(false);
			
			//*** clean card ***/
		    //try to remove counter file
			ds.selectPath("3f00:c010");
			ds.deleteFileOrDir(counterFileId);
		    //try to remove keyboard file
			ds.selectPath("3f00");
			ds.deleteFileOrDir(keyboardFileId);
		    //try to remove modhex mapping file
			ds.selectPath("3f00");
			ds.deleteFileOrDir(modhexFileId);
		    //try to remove hotp keyset
			ds.deleteKey(hotpKeyset.getVersion());
			
			System.out.println("Keyboard disabled successfully on this card.");
			
		}catch(Exception e){
			System.err.println(e.getMessage());
		}		
		
	}
	

	public static void main(String[] args) {
		
		
		//Uncomment and test
		
		//*** ============================================= ***//
		testDongleDetection(); //required for all tests
		testAuthentication(0xFF, false); //required for tests using authentication
		//*** ============================================= ***//
		//testModeSwitching(DaplugSession.WINUSB_DEVICE); //HID mode or WINUSB mode ; remove dongle then reinsert it after switching
		//*** ============================================= ***//
		//testGetSerial();
		//testGetStatus();
		//*** ============================================= ***//
		//testPutkey();
		//*** ============================================= ***//
		//Do an export, remove the dongle & reinsert it then do an import using the blob previously returned by the export.
		//testExportKey();
		//byte[] blob = DaplugUtils.hexStringToByteArray("CEFB53E8AC44C05BC48BB2D13C8B14744EB2027335C084ED9836C74ED8DBD82C95951E4E3772526A2F2D71E8F2DD12AB874EC23D40F15183B8AE5609E2C5FE96CB0F70EF78B169B75CB0B5E0BEC05261D8E2886AF4CC36C0");
		//testImportKey(blob);
		//*** ============================================= ***//
		//testFS();
		//*** ============================================= ***//
		//testEncDec();
		//*** ============================================= ***//
		//testGenerateRandom(20);
		//*** ============================================= ***//
		//testHmac();
		//testHotp();
		//testTotp();
		//*** ============================================= ***//
		//testKeyboard("http://www.plug-up.com/", true, DaplugKeyboard.HOTP_MODHEX, null);
		//testDisableKeyboard();
		//*** ============================================= ***//

	} 
}

