package com.fanniemae.automation.data;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.automation.SessionManager;
import com.fanniemae.automation.data.transforms.DataTransform;
import com.fanniemae.automation.data.transforms.TransformFactory;

/**
 * 
 * @author Richard Monson
 * @since 2016-01-20
 * 
 */
public class ExecutionPlanner {

	protected SessionManager _Session;

	public ExecutionPlanner(SessionManager session) {
		_Session = session;
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
			DataTransform currentTransform = TransformFactory.getTransform(_Session, eleTransform);
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
