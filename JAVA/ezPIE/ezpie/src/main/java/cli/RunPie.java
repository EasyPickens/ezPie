/**
 *  
 * Copyright (c) 2016 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
 */

package cli;

/**
 * 
 * @author Tara Tritt
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2016-06-08
 * 
 */

public class RunPie {
	
	private RunPie() {
		
	}
	
	public static void main(String[] args){
		new CLI(args).parse();
	}	
	
}
