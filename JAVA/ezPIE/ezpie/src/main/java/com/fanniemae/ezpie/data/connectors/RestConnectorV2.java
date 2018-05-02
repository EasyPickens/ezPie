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

package com.fanniemae.ezpie.data.connectors;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.FileUtilities;
import com.fanniemae.ezpie.common.RestRequestConfiguration;
import com.fanniemae.ezpie.common.RestUtilities;
import com.fanniemae.ezpie.common.RestUtilitiesV2;
import com.fanniemae.ezpie.common.StringUtilities;
import com.fanniemae.ezpie.common.XmlUtilities;
import com.fanniemae.ezpie.data.utilities.TreeNode;
import com.fanniemae.ezpie.datafiles.lowlevel.DataFileEnums.DataType;

/**
*
* @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
* @since 2018-01-20
* 
*/

public class RestConnectorV2 extends DataConnector {
	
	protected RestRequestConfiguration _requestConfig;
	protected NodeList _columns;

	public RestConnectorV2(SessionManager session, Element dataSource, Boolean isSchemaOnly) {
		super(session, dataSource, isSchemaOnly);
		
		_requestConfig = new RestRequestConfiguration();
		_requestConfig.setUrl(_session.requiredAttribute(dataSource, "URL"));
		
		String connectionName = _session.optionalAttribute(dataSource, "ConnectionName");
		if (StringUtilities.isNotNullOrEmpty(connectionName)) {
			Element conn = _session.getConnection(_connectionName);
			
			_requestConfig.setUsername(_session.optionalAttribute(conn, "Username"));
			_requestConfig.setPassword(_session.optionalAttribute(conn, "Password"));
			_requestConfig.setProxyHost(_session.optionalAttribute(conn, "ProxyHost"));
			_requestConfig.setProxyPort(_session.optionalAttribute(conn, "ProxyPort"));
			_requestConfig.setProxyUsername(_session.optionalAttribute(conn, "ProxyUsername"));
			_requestConfig.setProxyPassword(_session.optionalAttribute(conn, "ProxyPassword"));
			_requestConfig.setValidateCerfificate(StringUtilities.toBoolean(_session.optionalAttribute(conn, "ValidateCertificate", "True")));	
		}
		
		_columns = XmlUtilities.selectNodes(_dataSource, "*");
	}

	@Override
	public Boolean open() {
//		try {
//			String response = RestUtilitiesV2.sendRequest(_requestConfig);
//			int length = (response == null) ? 0 : response.length();
//			//_session.addLogMessage("", "RestConnector", String.format("View Response (%,d bytes)", length), "file://" + RestUtilities.writeResponseToFile(response, FileUtilities.getRandomFilename(_session.getLogPath(), "txt")));
//			_session.addLogMessage("", "RestConnector", String.format("View Raw Response (%,d bytes)", length), "file://" + FileUtilities.writeRandomTextFile(_session.getLogPath(), response));
//
//			int numColumns = _columns.getLength();
//			_session.addLogMessage("", "RestConnector", String.format("%,d columns found", numColumns));
//
//			// Read/create column names.
//			_dataSchema = new String[numColumns][2];
//			_dataTypes = new DataType[numColumns];
//
//			// get columns from definition file and split
//			ArrayList<TreeNode<String>> columns = new ArrayList<TreeNode<String>>();
//			ArrayList<String[]> tempPaths = new ArrayList<String[]>();
//			for (int i = 0; i < numColumns; i++) {
//				String columnName = _session.getAttribute(_columns.item(i), "Name");
//				String path = _session.getAttribute(_columns.item(i), "JsonPath");
//				String[] pathParts = path.split("\\.");
//				tempPaths.add(pathParts);
//				_dataSchema[i][0] = columnName;
//			}
//
//			// build tree that will contain all of the JSON attributes to return
//			for (int m = 0; m < tempPaths.size(); m++) {
//				boolean found = false;
//				for (int n = 0; n < columns.size(); n++) {
//					if (searchTree(columns.get(n), tempPaths.get(m), 0)) {
//						found = true;
//					}
//				}
//				if (!found) {
//					columns.add(new TreeNode<String>(tempPaths.get(m)[0]));
//					searchTree(columns.get(columns.size() - 1), tempPaths.get(m), 0);
//				}
//			}
//
//			TreeNode<String> root = new TreeNode<String>("root");
//			for (int n = 0; n < columns.size(); n++) {
//				root.addChild(columns.get(n));
//			}
//
//			Object json = new JSONTokener(response).nextValue();
//
//			// create rows by parsing JSON and using the tree with the attributes to search for
//			ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
//			if (json instanceof JSONObject) {
//				rows = buildRows((JSONObject) json, rows, root);
//			} else if (json instanceof JSONArray) {
//				ArrayList<ArrayList<String>> parentRows = new ArrayList<ArrayList<String>>(rows);
//				for (int i = 0; i < ((JSONArray) json).length(); i++) {
//					rows.addAll(buildRows(((JSONArray) json).getJSONObject(i), parentRows, root));
//				}
//			}
//
//			// determine data types and cast values to correct type
//			dataTypeRows(rows);
//
//		} catch (JSONException ex) {
//			throw new RuntimeException("Error while trying to make REST request: " + ex.getMessage(), ex);
//		}
		return true;
	}

	@Override
	public Boolean eof() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object[] getDataRow() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

}
