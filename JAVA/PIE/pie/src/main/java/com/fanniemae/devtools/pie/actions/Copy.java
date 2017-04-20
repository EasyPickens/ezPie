/**
 *  
 * Copyright (c) 2016 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPIE/wiki/Fannie-Mae-Open-Source-Licensing-Project
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
 */

package com.fanniemae.devtools.pie.actions;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.w3c.dom.Element;

import com.fanniemae.devtools.pie.SessionManager;
import com.fanniemae.devtools.pie.common.FileUtilities;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2016-05-31
 * 
 */

//@formatter:off
/**
 * 
 * Example Element:
 * <Copy 
 *    Source 
 *    Destination 
 *    IncludeFiles       - optional, defaults to all
 *    ExcludeFiles       - optional, defaults to none
 *    IncludeDirectories - optional, defaults to all
 *    ExcludeDirectories - optional, defaults to none
 *    ClearReadOnly      - optional, defaults to false
 *    SkipHidden         - optional, defaults to false
 *    Shallow            - optional, defaults to true
 * />
 *  
 */ 
//@formatter:on

public class Copy extends FileSystemAction {

	protected CopyOption[] _copyOptions = new CopyOption[] { StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES };

	public Copy(SessionManager session, Element action) {
		super(session, action);

		_source = requiredAttribute("Source", String.format("%s action requires a source directory or file.", _actionName));
		_destination = requiredAttribute("Destination", String.format("%s action requires a destination value.", _actionName));
		_countMessage = "copied";
	}

	@Override
	protected void processFile(String source, String destination, String nameOnly) {
		if (FileUtilities.isInvalidDirectory(destination)) {
			File file = new File(destination);
			file.mkdirs();
		}
		String destFilename = String.format("%s%s%s", destination, File.separator, nameOnly);
		try {
			Path sourcePath = Paths.get(source);
			Path destinationPath = Paths.get(destFilename);
			File destFile = new File(destFilename);
			if (_clearReadOnly && destFile.exists() && !destFile.canWrite()) {
				destFile.setWritable(true);
			}
			Files.copy(sourcePath, destinationPath, _copyOptions);
			_filesProcessed++;
		} catch (IOException e) {
			RuntimeException ex = new RuntimeException(String.format("Error while trying to copy %s to %s. Message is: %s", source, destFilename, e.getMessage()), e);
			if (FileUtilities.isValidFile(destFilename)) {
				File f = new File(destFilename);
				if (!f.canWrite()) {
					ex = new RuntimeException(String.format("Copied failed because existing destination file %s is marked as read-only.", destFilename));
				}
			}
			throw ex;
		}
	}
}
