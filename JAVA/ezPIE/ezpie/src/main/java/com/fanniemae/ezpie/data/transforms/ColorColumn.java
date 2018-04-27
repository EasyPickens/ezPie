package com.fanniemae.ezpie.data.transforms;

import java.util.Random;

import org.w3c.dom.Element;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.ColorUtilities;
import com.fanniemae.ezpie.common.PieException;
import com.fanniemae.ezpie.common.StringUtilities;

public class ColorColumn extends DataTransform {

	protected ColorUtilities _cu;

	protected double _hue;
	protected double _saturation = 0.5;
	protected double _value = 0.95;
	// private double _golden_ratio_conjugate = 0.618033988749895;

	public ColorColumn(SessionManager session, Element transform) {
		super(session, transform);
		_columnType = "java.lang.String";

		Random rand = new Random(System.nanoTime());
		_hue = readAttribute("Hue", rand.nextDouble());
		_saturation = readAttribute("Saturation", 0.5);
		_value = readAttribute("Value", 0.95);
		_cu = new ColorUtilities(_hue, _saturation, _value);
	}

	@Override
	public Object[] processDataRow(Object[] dataRow) {
		if (dataRow == null) {
			return dataRow;
		}
		dataRow = addDataColumn(dataRow);
		dataRow[_outColumnIndex] = _cu.getRGBString();
		_rowsProcessed++;
		return dataRow;
	}

	@Override
	public boolean isolated() {
		return false;
	}

	protected double readAttribute(String name, double defaultValue) {
		String value = getOptionalAttribute(name);
		double result = defaultValue;
		if (StringUtilities.isNotNullOrEmpty(value) && StringUtilities.isDouble(value)) {
			result = StringUtilities.toDouble(value);
			if ((result <= 0) && (result >= 1)) {
				throw new PieException(name + " attribute must be a decimal value between 0 and 1.");
			}
		}
		return result;
	}
}
