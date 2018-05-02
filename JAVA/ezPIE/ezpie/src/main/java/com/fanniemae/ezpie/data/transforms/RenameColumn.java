/**
 *  
 * Copyright (c) 2017 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE® is a registered trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.data.transforms;

import org.w3c.dom.Element;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.ArrayUtilities;
import com.fanniemae.ezpie.common.PieException;
import com.fanniemae.ezpie.common.ReportBuilder;
import com.fanniemae.ezpie.common.StringUtilities;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2017-09-12
 * 
*/

public class RenameColumn extends DataTransform {

	public RenameColumn(SessionManager session, Element transform) {
		super(session, transform, false);
	}

	@Override
	public Object[] processDataRow(Object[] dataRow) {
		return dataRow;
	}

	@Override
	public String[][] UpdateSchema(String[][] schema) {
		String[] newColumnNames = StringUtilities.split(getRequiredAttribute("NewColumnNames"));
		String[] inputColumnNames = StringUtilities.split(getRequiredAttribute("InputColumnNames"));
		
		if (newColumnNames.length != inputColumnNames.length) {
			throw new RuntimeException("The number of columns listed in both the InputColumnNames and NewColumnNames must be equal.");
		}

		_inputSchema = ArrayUtilities.cloneArray(schema);
		
		_transformInfo = new ReportBuilder();
		for(int i=0;i<inputColumnNames.length;i++) {
			int position = ArrayUtilities.indexOf(schema, inputColumnNames[i], true);
			if (position == -1) {
				throw new PieException(String.format("Did not find column \"%s\" in the input dataset.", inputColumnNames[i]));
			}
			schema[position][0] = newColumnNames[i];
			_transformInfo.appendFormatLine("%s ==> %s", inputColumnNames[i], newColumnNames[i]);
		}
		_session.addLogMessage("", _transformElementName, _transformInfo.toString());
		return schema;
	}
	
	@Override
	public void addTransformLogMessage() {
	}

}
