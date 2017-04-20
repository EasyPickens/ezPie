/**
 *  
 * Copyright (c) 2016 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPIE/wiki/Fannie-Mae-Open-Source-Licensing-Project
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
**/

package com.fanniemae.devtools.pie.actions.xmltransforms;

import java.io.File;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.fanniemae.devtools.pie.SessionManager;
import com.fanniemae.devtools.pie.common.FileUtilities;
import com.fanniemae.devtools.pie.common.StringUtilities;
import com.fanniemae.devtools.pie.common.XmlUtilities;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2016-09-20
 * 
 */

public class SaveFile extends XmlTransform {

	public SaveFile(SessionManager session, Element action, boolean isFolder) {
		super(session, action, isFolder);
	}

	@Override
	public Document execute(Document xmlDocument, File file) {
		String tempName = FileUtilities.getRandomFilename(_session.getStagingPath(), "xml");
		String filename = (_isFolder) ? _session.getAttribute(_action, "Filename") : optionalAttribute("Filename", tempName);
		String id = (_isFolder) ? _session.getAttribute(_action, "ID") : optionalAttribute("ID", "");

		// Resolve the data tokens for filename (if any)
		if ((file != null) && (filename.indexOf('@') > -1) && (filename.indexOf('~') > -1)) {
			String filenameNoExtension = FileUtilities.getFilenameOnly(file.getName());
			String filenameAndExtension = FileUtilities.getFilenameOnly(file.getName());
			String extension = "xml";
			String fullNameAndPath = file.getAbsolutePath();
			String justPath = file.getParent();
			//@formatter:off
			filename = filename.replace("@Data.FileNameNoExtension~", filenameNoExtension).replace("@Data.FullFileName~", filenameAndExtension)
					           .replace("@Data.FileExtension~", extension).replace("@Data.FileNameAndPath~", fullNameAndPath)
					           .replace("@Data.FilePathOnly~", justPath);
			//@formatter:on
		}

		XmlUtilities.SaveXmlDocument(filename, xmlDocument);
		if (StringUtilities.isNotNullOrEmpty(id)) {
			_session.addToken("LocalData", id, filename);
		}

		if (!_isFolder) {
			String xmlLogCopy = FileUtilities.writeRandomFile(_session.getLogPath(), "txt", XmlUtilities.XMLDocumentToString(xmlDocument));
			_session.addLogMessage("", "File Saved", "View Modified Xml", "file://" + xmlLogCopy);
		}
		return xmlDocument;
	}

}
