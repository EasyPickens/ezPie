/**
 *  
 * Copyright (c) 2016 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
**/

package com.fanniemae.ezpie.actions.xmltransforms;

import java.io.File;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.FileUtilities;
import com.fanniemae.ezpie.common.StringUtilities;
import com.fanniemae.ezpie.common.XmlUtilities;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2016-09-20
 * 
 */

public class SaveFile extends XmlTransform {

	protected String _tokenPrefix;
	protected String _tokenSuffix;

	public SaveFile(SessionManager session, Element action, boolean isFolder) {
		super(session, action, isFolder);
		_tokenPrefix = session.getTokenPrefix();
		_tokenSuffix = session.getTokenSuffix();
	}

	@Override
	public Document execute(Document xmlDocument, File file) {
		String randomFilename = FileUtilities.getRandomFilename(_session.getStagingPath(), "xml");
		String filename = (_isFolder) ? _session.getAttribute(_action, "Filename") : optionalAttribute("Filename", randomFilename);
		String tokenName = (_isFolder) ? _session.getAttribute(_action, "Name") : optionalAttribute("Name", "");

		// Resolve the data tokens for filename (if any)
		if ((file != null) && (filename.indexOf(_tokenPrefix) > -1) && (filename.indexOf(_tokenSuffix) > -1)) {
			String filenameNoExtension = FileUtilities.getFilenameOnly(file.getName());
			String filenameAndExtension = FileUtilities.getFilenameOnly(file.getName());
			String extension = "xml";
			String fullNameAndPath = file.getAbsolutePath();
			String justPath = file.getParent();
			//@formatter:off
			filename = filename.replace(_tokenPrefix + "Data.FileNameNoExtension" + _tokenSuffix, filenameNoExtension)
					           .replace(_tokenPrefix + "Data.FullFileName" + _tokenSuffix, filenameAndExtension)
					           .replace(_tokenPrefix + "Data.FileExtension" + _tokenSuffix, extension)
					           .replace(_tokenPrefix + "Data.FileNameAndPath" + _tokenSuffix, fullNameAndPath)
					           .replace(_tokenPrefix + "Data.FilePathOnly" + _tokenSuffix, justPath);
			//@formatter:on
		}

		XmlUtilities.saveXmlFile(filename, xmlDocument);
		if (StringUtilities.isNotNullOrEmpty(tokenName)) {
			_session.addToken("LocalData", tokenName, filename);
		}

		if (!_isFolder) {
			String xmlLogCopy = FileUtilities.writeRandomFile(_session.getLogPath(), "txt", XmlUtilities.xmlDocumentToString(xmlDocument));
			_session.addLogMessage("", "File Saved", "View Modified Xml", "file://" + xmlLogCopy);
		}
		return xmlDocument;
	}

}
