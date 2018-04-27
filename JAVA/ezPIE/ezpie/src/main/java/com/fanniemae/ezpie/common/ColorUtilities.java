/**
 *  
 * Copyright (c) 2018 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE® is a registered trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.common;

import java.util.Random;

import org.apache.commons.lang3.StringUtils;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2018-04-27
 * 
 */

public class ColorUtilities {

	private double _hue;
	private double _saturation = 0.5;
	private double _value = 0.95;
	private double _golden_ratio_conjugate = 0.618033988749895;

	public ColorUtilities() {
		Random rand = new Random();
		_hue = rand.nextDouble();
	}

	public ColorUtilities(double hue) {
		_hue = hue;
	}
	
	public ColorUtilities(double saturation, double value) {
		_saturation = saturation;
		_value = value;
	}

	public ColorUtilities(double hue, double saturation, double value) {
		_hue = hue;
		_saturation = saturation;
		_value = value;
	}
	
	public String[] getRGBArray(int length) {
		String[] colors = new String[length];
		for(int i=0;i<length;i++) {
			colors[i] = getRGBString();
		}
		return colors;
	}
	
	public String getRGBString() {
		_hue = _hue + _golden_ratio_conjugate;
		_hue = _hue % 1 ;
		return getRGB(_hue, _saturation, _value);
	}

	private String getRGB(double hue, double saturation, double value) {
		int hueFactor = (int) (hue * 6);
		double adjFactor = hue * 6 - hueFactor;
		double alternateA = value * (1 - saturation);
		double alternateB = value * (1 - adjFactor * saturation);
		double alternateC = value * (1 - (1 - adjFactor) * saturation);
		double percentRed = value;
		double percentGreen = alternateC;
		double percentBlue = alternateA;
		if (hueFactor == 0) {
			percentRed = value;
			percentGreen = alternateC;
			percentBlue = alternateA;
		} else if (hueFactor == 1) {
			percentRed = alternateB;
			percentGreen = value;
			percentBlue = alternateA;
		} else if (hueFactor == 2) {
			percentRed = alternateA;
			percentGreen = value;
			percentBlue = alternateC;
		} else if (hueFactor == 3) {
			percentRed = alternateA;
			percentGreen = alternateB;
			percentBlue = value;
		} else if (hueFactor == 4) {
			percentRed = alternateC;
			percentGreen = alternateA;
			percentBlue = value;
		} else {
			percentRed = value;
			percentGreen = alternateA;
			percentBlue = alternateB;
		}
		return "#" + toHexPair((int) (percentRed * 256)) + toHexPair((int) (percentGreen * 256)) + toHexPair((int) (percentBlue * 256));
	}

	private String toHexPair(int i) {
		if (i >= 256) {
			i = 255;
		}
		return StringUtils.leftPad(Integer.toHexString(i), 2, "0");
	}
}
