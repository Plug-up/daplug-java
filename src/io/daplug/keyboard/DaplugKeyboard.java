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
import io.daplug.utils.DaplugUtils;
import io.daplug.keyboard.IKeyboard;

public class DaplugKeyboard implements IKeyboard {

	private StringBuilder content = new StringBuilder();
	private int currentContentSize = 0;

	public DaplugKeyboard() {

	}

	/**
	 * void keyboard_addOSProbe(int nb, int delay, int code)
	 * 
	 * @param nb
	 *            int Number of Dongle reports (optional)
	 * @param delay
	 *            int Delay before Dongle report (optional)
	 * @param code
	 *            int Keycode sent in each report (optional)
	 * 
	 * @throws Exception
	 * @author yassir
	 */
	
	public void addOSProbe(int nb, int delay, int code) throws Exception {
		// TODO Auto-generated method stub
		int t_nb = 0;
		int t_delay = -1;
		int t_code = 0;
		
		if (nb == -1) t_nb = 0x10; else t_nb = nb;
		if (delay == -1) t_delay = 0xFFFF; else t_delay = delay;
		if (code != -1) t_code = code;
		
		String numberDongle = String.format("%02X", t_nb);
		String s_delay = String.format("%04X", t_delay); 
		String s_code = String.format("%02X", t_code); 
		
		int added_len = (new String("1004").length() + numberDongle.length()
				+ s_delay.length() + s_code.length()) / 2;

		if ((this.currentContentSize + added_len) <= MAX_KB_CONTENT_SIZE) {
			StringBuilder sb = new StringBuilder();
			sb.append("1004").append(numberDongle).append(s_delay)
					.append(s_code);
			this.content.append(sb);
			this.currentContentSize = this.currentContentSize + added_len;
		} else {
			throw new Exception("keyboard_addOSProbe(): Keyboard maximum content size exceeded !");
		}
	}

	/**
	 * 
	 * @param nb int numbrer of Dongle
	 * @param delay int dealy before dongle report 
 	 * @param code int
 	 * @throws Exception
 	 * @author yassir
	 */
	public void addOSProbeWinR(int nb, int delay, int code) throws Exception {
		// TODO Auto-generated method stub
		int t_nb = 0;
		int t_delay = -1;
		int t_code = 0x00;
		
		if (nb == -1) t_nb = 0x14; else t_nb = nb;
		if (delay == -1) t_delay = 0xFFFF; else t_delay = delay;
		if (code != -1) t_code = code;

		String nb_s = String.format("%02X", t_nb);
		String s_delay = String.format("%04X", t_delay); 
		String s_code = String.format("%02X", t_code); 

		int added_len = (new String("0204").length() + nb_s.length()
				+ s_delay.length() + s_code.length()) / 2;

		if ((this.currentContentSize + added_len) <= MAX_KB_CONTENT_SIZE) {
//			StringBuilder sb = new StringBuilder();
			this.content.append("0204").append(nb_s).append(s_delay)
					.append(s_code);
//			this.content.append(sb);
			this.currentContentSize = this.currentContentSize + added_len;
		} else {
			throw new Exception("addOSProbeWinR(): Keyboard maximum content size exceeded !");
		}
	}

	
	private void keyboard_add(String hexa, String name)  throws Exception {
		// TODO Auto-generated method stub

		int added_len = hexa.length() / 2;

		if ((this.currentContentSize + added_len) <= MAX_KB_CONTENT_SIZE) {
			this.content.append(hexa);
			this.currentContentSize = this.currentContentSize + added_len;
		} else {
			throw new Exception(name
					+ "(): Keyboard maximum content size exceeded !");
		}
	}

	/**
	 * check if the OS is a Microsoft Windows
	 * @author yassir
	 */
	public void addIfPC() {
		try {
			this.keyboard_add("0E00", "addIfPC");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * check if the OS is an Apple OS X
	 * @author yassir
	 */
	public void addIfMac() {
		try {
			this.keyboard_add("0F00", "addIfMac");
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * add an ascii text to keychains
	 * @param text String
	 * @author yassir
	 */
	public void addAsciiText(String text) {
		// TODO Auto-generated method stub
		String hex_text = DaplugUtils.asciiToHex(text);
		try {
			this.keyboard_add(hex_text, "addAsciiText");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * add text to an Windows OS, to keychains
	 * @throws Exception
	 * @param text String
	 */
	public void addTextWindows(String text) throws Exception  {
		// TODO Auto-generated method stub
		int nb =0, 
			added_len,
			i = 0;
		
		String part = "";
		
		String mwtl_s = String.format("%02X", MAX_WINDOWS_TEXT_LEN);
		
		int last_part_len = text.length() % MAX_WINDOWS_TEXT_LEN; 
		String last_part_len_s = String.format("%02X", last_part_len);
		
		if (last_part_len == 0)
			nb = text.length() / MAX_WINDOWS_TEXT_LEN;
		else
			nb = (int) text.length() / MAX_WINDOWS_TEXT_LEN + 1;
		while (nb > 0) {
			added_len = (new String("04").length() + mwtl_s.length()) / 2;
			if (nb > 1 || (last_part_len == 0) ) {
				if ((this.currentContentSize + added_len) <= MAX_KB_CONTENT_SIZE) {
//					StringBuilder sb = new StringBuilder();
					this.content.append("04")
					  .append(mwtl_s);
//					this.content.append(sb);
					part = text.substring(i, i + MAX_WINDOWS_TEXT_LEN - 1);
					this.addAsciiText(part);
					part = "";
					this.currentContentSize = this.currentContentSize
							+ added_len;
				} else {
					throw new Exception ("addTextWindows(): Keyboard maximum content size exceeded !");
				}
			} else {
				if ((this.currentContentSize + added_len) <= MAX_KB_CONTENT_SIZE) {
//					StringBuilder sb = new StringBuilder();
					this.content.append("04").append(last_part_len_s);
//					this.content.append(sb);
					part = text.substring(i, i
							+ last_part_len );
					this.addAsciiText(part);
					part = "";
					this.currentContentSize = this.currentContentSize
							+ added_len;
				} else {
					throw new Exception ("addTextWindows(): Keyboard maximum content size exceeded !");
				}
			}
			i = i + MAX_WINDOWS_TEXT_LEN;
			nb--;
		}
	}

	/**
	 * add Text to an Apple OS, OS X. the azerty mode is deactivate when user set 
	 * azerty = -1. The delay let the OS open the command given by the plug-up, and
	 * make the daplug Dongle in standby mode. 
	 * @param text String
	 * @param azerty int
	 * @param delay int
	 * @throws Exception
	 * @author yassir
	 */
	public void addTextMac(String text, int azerty, int delay) throws Exception  {
		// TODO Auto-generated method stub
		int len_text = text.length(),
				added_len,
				nb = 0,
				i  = 0;
		
		int t_azerty = 0, 
			t_delay = 0x1000;
		
		if (azerty != -1) t_azerty = azerty;
		if (delay != -1) t_delay = delay;
		
		int last_part_len = len_text % MAX_MAC_TEXT_LEN;
		String last_part_len_s = String.format("%02X", last_part_len + 3);
		String mmtl_s 	= String.format("%02X",(MAX_MAC_TEXT_LEN + 3));
		String azerty_s = String.format("%02X",t_azerty);
		String delay_s  = String.format("%04X", t_delay);
		
		String part = "";

		if (last_part_len == 0)
			nb = len_text / MAX_MAC_TEXT_LEN;
		else
			nb = (len_text / MAX_MAC_TEXT_LEN) + 1;
		while (nb > 0) {
			added_len = (new String("11").length() + mmtl_s.length()
					+ azerty_s.length() + delay_s.length()) / 2;
			if (nb > 1 || last_part_len == 0) {
				if ((this.currentContentSize + added_len) <= MAX_KB_CONTENT_SIZE) {
//					StringBuilder sb = new StringBuilder();
					this.content.append("11").append(mmtl_s).append(azerty_s)
							.append(delay_s);
//					this.content.append(sb);
					part = text.substring(i, i + MAX_MAC_TEXT_LEN - 1);
					this.addAsciiText(part);
					part = "";
					this.currentContentSize += added_len;
				} else {
					throw new Exception ("addTextMac(): Keyboard maximum content size exceeded !");
				}
			} else {
				if ((this.currentContentSize + added_len) <= MAX_KB_CONTENT_SIZE) {
//					StringBuilder sb = new StringBuilder();
					this.content.append("11").append(last_part_len_s).append(azerty_s)
					.append(delay_s);
//					this.content.append(sb);
					part = text.substring(i, i
							+ (text.length() % MAX_MAC_TEXT_LEN) );
					this.addAsciiText(part);
					part = "";
					this.currentContentSize += added_len;
				} else {
					throw new Exception ("addTextMac(): Keyboard maximum content size exceeded !");
				}
			}
		     i = i + MAX_MAC_TEXT_LEN;
		     nb--;
		}
	}

	/**
	 * 
	 * @param code String
	 * @throws Exception
	 */
	public void addKeyCodeRaw(String code) throws Exception  {
		// TODO Auto-generated method stub
		
		int len_code = code.length() / 2;
		String len_code_s = String.format("%02X",len_code);
		
		int added_len = (new String("09").length() + len_code_s.length() + code.length()) / 2;
		
		if ((this.currentContentSize + added_len) <= MAX_KB_CONTENT_SIZE) {
//			StringBuilder sb = new StringBuilder();
			this.content.append("09").append(len_code_s).append(code);
//			this.content.append(sb);
			this.currentContentSize += added_len;
		}else {
			throw new Exception ("addKeyCodeRaw(): Keyboard maximum content size exceeded !");
		}
	}

	/**
	 * 
	 * @param code String
	 * @throws Exception
	 */
	public void addKeyCodeRelease(String code) throws Exception  {
		// TODO Auto-generated method stub
		int len_code = code.length() / 2;
		String len_code_s = String.format("%02X", len_code);
		
		int added_len = (new String("03").length() + len_code_s.length() + code.length()) / 2;
		
		if ((this.currentContentSize + added_len) <= MAX_KB_CONTENT_SIZE) {
//			StringBuilder sb = new StringBuilder();
			this.content.append("03").append(len_code_s).append(code);
//			this.content.append(sb);
			this.currentContentSize += added_len;
		}else {
			throw new Exception ("addKeyCodeRelease(): Keyboard maximum content size exceeded !");
		}

	}

	/**
	 * add a HOTP generator to keychains. this HOTP generator, create an hotp string, everytime you 
	 * insert daplug dongle to USB port. 
	 * @param options int 
	 * @param digitsNb int the hotp data length to generate
	 * @param keysetVersion the HOTP keyset ID
	 * @param counterFileId int the counter file ID
	 * @param div String diversifiant used
	 * @throws Exception
	 * @author yassir
	 */
	public void addHotpCode(int options, int digitsNb, int keysetVersion,
			int counterFileId, String div) throws Exception  {
		// TODO Auto-generated method stub
		
		String flag_s 	  = String.format("%02X", options),
			   digitsNb_s = String.format("%02X", digitsNb),
			   ksv_s      = String.format("%02X", keysetVersion),
			   cfi_s	  = String.format("%04X",counterFileId);
			   
		StringBuilder sb = new StringBuilder();
		sb.append(flag_s).append(digitsNb_s).append(ksv_s);
		
		String tmp = sb.toString();
		if (!div.equals("")) {
			if (DaplugUtils.isHexInput(div) && (div.length() / 2) == 16) {
				sb.append(div);
				tmp = sb.toString();
			}else {
				throw new Exception("addHotpCode():  Invalid diversifier !");
			}
		}
		
		tmp = sb.append(cfi_s).toString();
		
		String len_tmp_s = String.format("%02X",(tmp.length() / 2));
		
		int added_len = (new String("50").length() + len_tmp_s.length() + tmp.length()) / 2;
		
		if ((this.currentContentSize + added_len) <= MAX_KB_CONTENT_SIZE) {
			StringBuilder sb_1 = new StringBuilder();
			sb_1.append("50").append(len_tmp_s).append(tmp);
			this.content.append(sb_1.toString());
			this.currentContentSize += added_len;
		}else {
			throw new Exception ("addHotpCode(): Keyboard maximum content size exceeded !");
		}
	}

	/**
	 *	add a return "ENTER" hit to keychain 	
	 */
	public void addReturn() throws Exception {
		// TODO Auto-generated method stub
		int added_len = new String("0D00").length() / 2;
		if ((this.currentContentSize + added_len) <= MAX_KB_CONTENT_SIZE) {
			this.content.append("0D00");
			this.currentContentSize += added_len;
		}else {
			throw new Exception ("addReturn(): Keyboard maximum content size exceeded !");
		}
	}

	/**
	 * make the daplugDongle sleep for duration. This sleep is actually available only when you
	 * use the keyboard. 
	 * It usually used to let the host to perform the command given by the Daplug Dongle
	 * @param duration int
	 * @throws Exception
	 * @author yassir
	 */
	public void addSleep(int duration) throws Exception {
		// TODO Auto-generated method stub
		String duration_s = "";
		int t_duration = 0xFFFF;
		if(duration != -1)
			t_duration = duration;
		if (duration > 0xFFFF)
			duration_s =String.format("%08X", t_duration);
		else 
			duration_s = String.format("%04X", t_duration);
		int added_len = (new String("0104").length() + duration_s.length()) /2 ;
		if ((this.currentContentSize + added_len) <= MAX_KB_CONTENT_SIZE) {
			StringBuilder sb = new StringBuilder();
			if (duration > 0xFFFF)
				sb.append("0104");
			else
				sb.append("0102");
			sb.append(duration_s);
			this.content.append(sb.toString());
			this.currentContentSize += added_len;
		}
		else {
			throw new Exception("addSleep(): Keyboard maximum content size exceeded !");
		}
	}
	
	
	/**
	 * zero Padding add 0x00 to Content
	 * @param size 
	 * @throws Exception
	 * @author yassir
	 */
	public void zeroPad(int size) throws Exception  {
		// TODO Auto-generated method stub
		if (size <= this.currentContentSize) {
			throw new Exception ("zeroPad() - Keyboard content size exceeded !");
		}
		while (size > this.currentContentSize) {
			if ((this.currentContentSize + 1) <= MAX_KB_CONTENT_SIZE) {
				this.content.append("00");
				this.currentContentSize = this.currentContentSize + 1;
			} else {
				throw new Exception ("keyboard_zeroPad(): Keyboard maximum content size exceeded !");
			}
		}
	}
	
	// getters and setters
	public String getContent() {
		return content.toString().toUpperCase();
	}

	public void setContent(String content) {
		this.content.append(content);
	}

	public int getCurrentContentSize() {
		return currentContentSize;
	}

	public void setCurrentContentSize(int currentContentSize) {
		this.currentContentSize = currentContentSize;
	}

}
