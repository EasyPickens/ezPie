/**
 *  
 * Copyright (c) 2016 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE® is a registered trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.actions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.w3c.dom.Element;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.FileUtilities;
import com.fanniemae.ezpie.common.PieException;
import com.fanniemae.ezpie.common.StringUtilities;

/**
 * 
 * @author Rick Monson (https://www.linkedin.com/in/rick-monson/)
 * @since 2016-12-02
 * 
 */

public class VerifyJavaFiles extends Delete {

	protected int _maxSize = -1;
	protected String _fileContents = "";

	public VerifyJavaFiles(SessionManager session, Element action) {
		super(session, action);
		_maxSize = StringUtilities.toInteger(optionalAttribute("FileSizeLimit", "-1"), -1);
		_sb.append("Verifying that JAVA files contain uncommented code. \n");
	}

	@Override
	protected void processFile(String source, String destination, String nameOnly) {
		try {
			File sourceFile = new File(source);
			if ((source == null) || !source.toLowerCase().endsWith(".java")) {
				return;
			}

			if (!hasCode(source)) {
				if (_clearReadOnly && !sourceFile.canWrite()) {
					sourceFile.setWritable(true);
				}
				_filesProcessed++;
				_sb.append(String.format("Deleting file %s (it contains no code)\n", sourceFile));
				sourceFile.delete();
			}
		} catch (Exception e) {
			RuntimeException ex = new PieException(String.format("Error while trying to delete %s. Message is %s", source, e.getMessage()), e);
			throw ex;
		}
	}

	protected boolean hasCode(String filename) {
		_fileContents = FileUtilities.loadFile(filename);
		if (_fileContents == null)
			return false;

		_fileContents = _fileContents.trim();
		if (!_fileContents.startsWith("/*") && !_fileContents.startsWith("//"))
			return true;
		
		removeComments("/*","*/");
		removeComments("//","\n");
		_fileContents = _fileContents.trim();
		return _fileContents.trim().length() > 0;
	}

	protected void removeComments(String commentStart, String commentEnd) {
		String comment = "";
		int lastLength = -1;

		int startComment = _fileContents.indexOf(commentStart);
		while (startComment > -1) {
			if (startComment >= 0) {
				int endCommentLine = _fileContents.indexOf(commentEnd, startComment);
				if (endCommentLine > -1) {
					comment = _fileContents.substring(startComment, endCommentLine + 1);
					_fileContents = _fileContents.replace(comment, "");
				} else {
					endCommentLine = _fileContents.indexOf("\n", startComment);
					if (endCommentLine > -1) {
						comment = _fileContents.substring(startComment, endCommentLine + 1);
						_fileContents = _fileContents.replace(comment, "");
					} else {
						comment = _fileContents.substring(startComment, _fileContents.length());
						_fileContents = _fileContents.replace(comment, "");
					}
				}
			}
			if (lastLength == _fileContents.length())
				break;			
			lastLength = _fileContents.length();
			startComment = _fileContents.indexOf(commentStart, startComment);			
		}
	}

	protected String readFile(String filename) {
		File file = new File(filename);
		if ((_maxSize != -1) && (file.length() > _maxSize)) {
			return null;
		}

		char[] buffer = null;
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			buffer = new char[(int) file.length()];

			int i = 0;
			int c = br.read();
			while (c != -1) {
				buffer[i++] = (char) c;
				c = br.read();
			}

		} catch (IOException e) {
			throw new PieException(String.format("Error while trying to verify %s JAVA file has uncommented code lines.", filename), e);
		}

		return new String(buffer);
	}
}
