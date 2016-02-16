package com.fanniemae.automation.actions;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.automation.SessionManager;
import com.fanniemae.automation.common.DataStream;
import com.fanniemae.automation.common.XmlUtilities;
import com.fanniemae.automation.data.DataEngine;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-21
 * 
 */
public class DataSet extends Action {

	public DataSet(SessionManager session, Element action) {
		super(session, action);
	}

	@Override
	public String execute() {
		DataEngine de = new DataEngine(_Session);
		NodeList nl = XmlUtilities.selectNodes(_Action, "DataSource");
		
		int len = nl.getLength();
		if (len == 0) {
			throw new RuntimeException("Each DataSet element requires at least one DataSource child element.");
		}
		
		for (int i = 0; i < len; i++) {
			DataStream ds = de.getData((Element) (nl.item(i)));
			_Session.addDataSet(_ID,ds);
		}
		return null;
	}

}
