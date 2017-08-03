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
 * @since 2017-08-03
 * 
 */

public final class Constants {

	public static final int ENCRYPTED_PREFIX_LENGTH = 10;
	public static final String ENCRYPTED_PREFIX = "{ENCRYPT1}";

	public static final int SECURE_SUFFIX_LENGTH = 6;
	public static final String SECURE_SUFFIX = "Secure";

	public static final int HIDE_SUFFIX_LENGTH = 4;
	public static final String HIDE_SUFFIX = "Hide";

	public static final String VALUE_HIDDEN_MESSAGE = "-- Value Hidden --";
	public static final String LOG_WARNING_MESSAGE = "** Warning **";
	public static final String TOKEN_TYPES_RESERVED = "|configuration|system|environment|application|data|";

	private Constants() {
	}
}
