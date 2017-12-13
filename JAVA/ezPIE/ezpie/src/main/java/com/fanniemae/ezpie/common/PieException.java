/**
 *  
 * Copyright (c) 2017 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
**/

package com.fanniemae.ezpie.common;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2017-11-22
 * 
 */

public class PieException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public PieException() {
		super();
	}

	public PieException(String message) {
		super(message);
	}

	public PieException(Throwable cause) {
		super(cause);
	}

	public PieException(String message, Throwable cause) {
		super(message, cause);
	}

	public PieException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
