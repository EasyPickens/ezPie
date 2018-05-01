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

	private Random _rand = new Random();
	private double _hue = _rand.nextDouble();
	private double _saturation = 0.5;
	private double _brightness = 0.95;
	private double _golden_ratio_conjugate = 0.618033988749895;

	public ColorUtilities() {
	}

	public ColorUtilities(double hue) {
		_hue = hue;
	}

	public ColorUtilities(double saturation, double brightness) {
		_saturation = saturation;
		_brightness = brightness;
	}

	public ColorUtilities(String hue, String saturation, String brightness) {
		if (hue != null) {
			double dHue = StringUtilities.toDouble(hue, -1.0);
			if ((dHue >= 0) && (dHue <= 1)) {
				_hue = dHue;
			}
		}
		
		if (saturation != null) {
			double dSaturation = StringUtilities.toDouble(saturation, -1.0);
			if ((dSaturation >= 0) && (dSaturation <= 1)) {
				_saturation = dSaturation;
			}
		}
		
		if (brightness != null) {
			double dBrightness = StringUtilities.toDouble(brightness, -1.0);
			if ((dBrightness >= 0) && (dBrightness <= 2)) {
				System.out.println(dBrightness);
				_brightness = dBrightness;
			}
		}
	}

	public ColorUtilities(double hue, double saturation, double brightness) {
		_hue = hue;
		_saturation = saturation;
		_brightness = brightness;
	}

	public String[] getRGBArray(int length) {
		String[] colors = new String[length];
		for (int i = 0; i < length; i++) {
			colors[i] = getRGBString();
		}
		return colors;
	}

	public String getRGBString() {
		_hue = _hue + _golden_ratio_conjugate;
		_hue = _hue % 1;
		return getRGB(_hue, _saturation, _brightness);
	}

	private String getRGB(double hue, double saturation, double brigthness) {
		int hueFactor = (int) (hue * 6);
		double adjFactor = hue * 6 - hueFactor;
		double alternateA = brigthness * (1 - saturation);
		double alternateB = brigthness * (1 - adjFactor * saturation);
		double alternateC = brigthness * (1 - (1 - adjFactor) * saturation);
		double percentRed = brigthness;
		double percentGreen = alternateC;
		double percentBlue = alternateA;
		if (hueFactor == 0) {
			percentRed = brigthness;
			percentGreen = alternateC;
			percentBlue = alternateA;
		} else if (hueFactor == 1) {
			percentRed = alternateB;
			percentGreen = brigthness;
			percentBlue = alternateA;
		} else if (hueFactor == 2) {
			percentRed = alternateA;
			percentGreen = brigthness;
			percentBlue = alternateC;
		} else if (hueFactor == 3) {
			percentRed = alternateA;
			percentGreen = alternateB;
			percentBlue = brigthness;
		} else if (hueFactor == 4) {
			percentRed = alternateC;
			percentGreen = alternateA;
			percentBlue = brigthness;
		} else {
			percentRed = brigthness;
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
