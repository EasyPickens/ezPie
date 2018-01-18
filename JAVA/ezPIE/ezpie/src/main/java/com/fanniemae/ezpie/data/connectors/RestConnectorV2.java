package com.fanniemae.ezpie.data.connectors;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.RestRequestConfiguration;
import com.fanniemae.ezpie.common.StringUtilities;
import com.fanniemae.ezpie.common.XmlUtilities;

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
		// TODO Auto-generated method stub
		return null;
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
