/**
 *  
 * Copyright (c) 2015 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE® is a registered trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.data;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.data.transforms.DataTransform;
import com.fanniemae.ezpie.data.transforms.TransformFactory;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2016-01-20
 * 
 */
public class ExecutionPlanner {

	protected SessionManager _session;

	protected boolean _localCacheEnabled = true;
	protected int _localCacheMinutes = 30;

	public ExecutionPlanner(SessionManager session, boolean localCacheEnabled, int localCacheMinutes) {
		_session = session;
		_localCacheEnabled = localCacheEnabled;
		_localCacheMinutes = localCacheMinutes;
	}

	public Map<Integer, Map<Integer, DataTransform>> getExecutionPlan(NodeList transforms) {
		Map<Integer, Map<Integer, DataTransform>> processingGroups = new HashMap<Integer, Map<Integer, DataTransform>>();
		Map<Integer, DataTransform> currentTransformGroup = new HashMap<Integer, DataTransform>();
		int iLen = transforms.getLength();

		if (iLen == 0) {
			// No data transforms to apply, just read data.
			processingGroups.put(0, new HashMap<Integer, DataTransform>());
			return processingGroups;
		}

		for (int i = 0; i < iLen; i++) {
			Element eleTransform = (Element) transforms.item(i);
			DataTransform currentTransform = TransformFactory.getTransform(_session, eleTransform);
			currentTransform.setLocalCacheConfiguration(_localCacheEnabled, _localCacheMinutes);
			if (currentTransform.isolated()) {
				if ((processingGroups.size() == 0) && (currentTransformGroup.size() == 0)) {
					processingGroups.put(processingGroups.size(), new HashMap<Integer,DataTransform>());
				}
				if (currentTransformGroup.size() > 0) {
					// put the previous transforms into a processing group
					processingGroups.put(processingGroups.size(), currentTransformGroup);
					// create the next processing group and add the isolated transform
					currentTransformGroup = new HashMap<Integer, DataTransform>();
				}
				currentTransformGroup.put(currentTransformGroup.size(), currentTransform);
				processingGroups.put(processingGroups.size(), currentTransformGroup);
				// finally create the container for the next processing group
				currentTransformGroup = new HashMap<Integer, DataTransform>();
				continue;
			}
			currentTransformGroup.put(currentTransformGroup.size(), currentTransform);
		}
		if (currentTransformGroup.size() > 0) {
			processingGroups.put(processingGroups.size(), currentTransformGroup);
		} else if (processingGroups.size() == 0) {
			processingGroups.put(0, new HashMap<Integer, DataTransform>());
		}
		return processingGroups;
	}
}
