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

	// @formatter:off
	// Just an array of 50 randomly generated colors, can use these, assign their own, or generate random stuff.
	private final String[] _colorQueue = new String[] { "#79f3a4", "#7995f3", "#f379dc", "#b8f379", "#f38179", "#79f3e6", 
			                                            "#f3c379", "#9f79f3", "#79f37c", "#f3799a", "#79bdf3", "#e1f379", 
			                                            "#e179f3", "#79f3be", "#f39a79", "#797cf3", "#9ff379", "#f379c3", 
			                                            "#79e6f3", "#f3dc79", "#b879f3", "#79f395", "#f37981", "#79a4f3", 
			                                            "#c8f379", "#f379eb", "#79f3d7", "#f3b379", "#9079f3", "#86f379", 
			                                            "#f379a9", "#79cdf3", "#f0f379", "#d279f3", "#79f3ae", "#f38b79", 
			                                            "#798bf3", "#aff379", "#f379d2", "#79f3f0" };
	// @formatter:on
	private int _queueIndex = 0;

	private Random _rand = new Random();
	private double _hue = _rand.nextDouble();
	private double _saturation = 0.5;
	private double _brightness = 0.95;
	private double _golden_ratio_conjugate = 0.618033988749895;
	private boolean _randomColor = false;

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

	public void useRandomColor(boolean value) {
		_randomColor = value;
	}

	public String[] getColorArray(int length) {
		if (_randomColor) {
			return generateRandomColorArray(length);
		}
		return buildColorArray(length);
	}

	public String nextColor() {
		if (_randomColor) {
			return randomColorString();
		}
		return colorString();
	}

	protected String[] buildColorArray(int length) {
		String[] colors = new String[length];
		for (int i = 0; i < length; i++) {
			colors[i] = colorString();
		}
		return colors;
	}

	protected String[] generateRandomColorArray(int length) {
		String[] colors = new String[length];
		for (int i = 0; i < length; i++) {
			colors[i] = randomColorString();
		}
		return colors;
	}

	protected String colorString() {
		String result = _colorQueue[_queueIndex];
		_queueIndex++;
		if (_queueIndex >= _colorQueue.length) {
			_queueIndex = 0;
		}
		return result;
	}

	protected String randomColorString() {
		_hue = _hue + _golden_ratio_conjugate;
		_hue = _hue % 1;
		return generateRandomColor(_hue, _saturation, _brightness);
	}

	protected String generateRandomColor(double hue, double saturation, double brigthness) {
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

	protected String toHexPair(int i) {
		if (i >= 256) {
			i = 255;
		}
		return StringUtils.leftPad(Integer.toHexString(i), 2, "0");
	}
}
