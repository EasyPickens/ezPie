package com.fanniemae.automation.data;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.automation.SessionManager;
import com.fanniemae.automation.data.transforms.DataTransform;
import com.fanniemae.automation.data.transforms.SequenceColumn;
import com.fanniemae.automation.data.transforms.Sort;
import com.fanniemae.automation.data.transforms.TimespanColumn;

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

		//_Session.addLogMessage("Execution Path", "Initialize", "Initialize data transformation processing groups.");
		//_Session.addLogMessage("", "Processing Group #1", "");
		for (int i = 0; i < iLen; i++) {
			Element eleTransform = (Element) transforms.item(i);
			String nodeName = eleTransform.getNodeName();
			DataTransform currentTransform = null;

			switch (nodeName) {
			case "TimespanColumn":
				currentTransform = new TimespanColumn(_Session, eleTransform);
				break;
			case "SequenceColumn":
				currentTransform = new SequenceColumn(_Session, eleTransform);
				break;
			case "Sort":
				currentTransform = new Sort(_Session, eleTransform);
				break;
			default:
				throw new RuntimeException(String.format("%s data transformation not currently supported.", nodeName));
			}
			if (currentTransform.isTableLevel()) {
//				if (aCurrentGroup.size() == 0) {
//					_Session.addLogMessage("", "", "Import the data into the processing engine.");
//				}
				processingGroups.put(processingGroups.size(), aCurrentGroup);
				aCurrentGroup = new HashMap<Integer, DataTransform>();
//				if (i < iLen) {
//					_Session.addLogMessage("", String.format("Processing Group #%d", processingGroups.size() + 1), "");
//				}
			}
			aCurrentGroup.put(aCurrentGroup.size(), currentTransform);
		}
		if (aCurrentGroup.size() > 0) {
			processingGroups.put(processingGroups.size(), aCurrentGroup);
		}

		// if (processingGroups.size() == 1) {
		// _Session.addLogMessage("", "Execution Path",
		// "Transformations applied as data is read from the source.");
		// } else {
		// _Session.addLogMessage("", "Execution Path",
		// String.format("Transformations require %d processing groups.",
		// processingGroups.size()));
		// }

		// Output processing group information to the log.
//		for (int i = 0; i < processingGroups.size(); i++) {
//			_Session.addLogMessage("", String.format("Processing Group #%d", i + 1), "");
//			int steps = processingGroups.get(i).size();
//			if (steps == 0) {
//				 //_Session.addLogMessage("", "Execution Path", "Data is read from the source.");
//			} else {
//				for (int x = 0; x < processingGroups.get(i).size(); x++) {
//					processingGroups.get(i).get(x).addTransformLogMessage();
//				}
//			}
//		}

		return processingGroups;
	}
}
