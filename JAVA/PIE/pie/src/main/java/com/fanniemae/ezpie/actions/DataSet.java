/**
 *  
 * Copyright (c) 2015 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPIE/wiki/Fannie-Mae-Open-Source-Licensing-Project
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.actions;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.DataStream;
import com.fanniemae.ezpie.common.XmlUtilities;
import com.fanniemae.ezpie.data.DataEngine;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2015-12-21
 * 
 */

public class DataSet extends Action {

	public DataSet(SessionManager session, Element action) {
		super(session, action);
	}

	@Override
	public String executeAction() {
		DataEngine de = new DataEngine(_session);
		NodeList nl = XmlUtilities.selectNodes(_action, "DataSource");
		
		int len = nl.getLength();
		if (len == 0) {
			throw new RuntimeException("Each DataSet element requires at least one DataSource child element.");
		}
		
		for (int i = 0; i < len; i++) {
			DataStream ds = de.getData((Element) (nl.item(i)));
			_session.addDataSet(_id,ds);
		}
		return null;
	}

}
