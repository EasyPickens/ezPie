package com.fanniemae.devtools.pie.data;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.devtools.pie.SessionManager;
import com.fanniemae.devtools.pie.data.transforms.DataTransform;
import com.fanniemae.devtools.pie.data.transforms.TransformFactory;

/**
 * 
 * @author Richard Monson
 * @since 2016-01-20
 * 
 */
public class ExecutionPlanner {

	protected SessionManager _session;

	public ExecutionPlanner(SessionManager session) {
		_session = session;
	}

	public Map<Integer, Map<Integer, DataTransform>> getExecutionPlan(NodeList transforms) {
		Map<Integer, Map<Integer, DataTransform>> processingGroups = new HashMap<Integer, Map<Integer, DataTransform>>();
		Map<Integer, DataTransform> aCurrentGroup = new HashMap<Integer, DataTransform>();
		int iLen = transforms.getLength();

		if (iLen == 0) {
			// No data transforms to apply, just read data.
			processingGroups.put(0, new HashMap<Integer, DataTransform>());
			return processingGroups;
		}

		for (int i = 0; i < iLen; i++) {
			Element eleTransform = (Element) transforms.item(i);
			DataTransform currentTransform = TransformFactory.getTransform(_session, eleTransform);
			if (currentTransform == null) {
				continue;
			}else if (currentTransform.isTableLevel()) {
				processingGroups.put(processingGroups.size(), aCurrentGroup);
				aCurrentGroup = new HashMap<Integer, DataTransform>();
			}
			aCurrentGroup.put(aCurrentGroup.size(), currentTransform);
		}
		if (aCurrentGroup.size() > 0) {
			processingGroups.put(processingGroups.size(), aCurrentGroup);
		} else if (processingGroups.size() == 0) {
			processingGroups.put(0, new HashMap<Integer, DataTransform>());
		}
		return processingGroups;
	}
}
