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

import io.daplug.exception.DaplugException;

/**
 * I/O Exception accessing a Plug-up dongle
 * @author nba
 *
 */
public class DaplugCommunicationException extends DaplugException {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1809956334171889105L;

	/**
	 * I/O exception with a reason
	 * @param reason reason
 	 */
	public DaplugCommunicationException(String reason) {
		super(reason);
	}
	
	/**
	 * I/O exception with a cause
	 * @param cause parent exception
	 */
	public DaplugCommunicationException(Throwable cause) {
		super("I/O error", cause);
	}

}
