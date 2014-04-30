/******************************************************************************
 file           : $Id$
 project        : Plug-up v2 API
 author         : $Author$
 ------------------------------------------------------------------------------
 changed on     : $Revision$
 ------------------------------------------------------------------------------
 description    : Plug-up v2 API
 ------------------------------------------------------------------------------
 Copyright (c) 2012 Ubinity SAS. All rights reserved.
 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 ******************************************************************************/

package io.daplug.exception;

	/**
	 * Generic exception class for Plug-up API
	 * @author nba
	 *
	 */
	public class DaplugException extends Exception {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = -1690914693287741177L;

		/**
		 * Exception with a reason string
		 * @param reason reason string
		 */
		public DaplugException(String reason) {
			super(reason);
		}
		
		/**
		 * Exception with a reason string and a cause
		 * @param reason reason string
		 * @param cause parent exception
		 */
		public DaplugException(String reason, Throwable cause) {
			super(reason, cause);
		}
		
}
