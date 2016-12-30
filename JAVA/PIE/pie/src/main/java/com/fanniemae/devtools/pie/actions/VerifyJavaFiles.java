package com.fanniemae.devtools.pie.actions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.w3c.dom.Element;

import com.fanniemae.devtools.pie.SessionManager;
import com.fanniemae.devtools.pie.common.FileUtilities;
import com.fanniemae.devtools.pie.common.StringUtilities;

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
			RuntimeException ex = new RuntimeException(String.format("Error while trying to delete %s. Message is %s", source, e.getMessage()), e);
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

//		String comment = "";
//		int lastStart = -1;
//		int lastEnd = -1;
//		int startCommentBlock = fileContents.indexOf("/*");
//		while (startCommentBlock > -1) {
//			if (startCommentBlock >= 0) {
//				int endCommentBlock = fileContents.indexOf("*/", startCommentBlock);
//				if (endCommentBlock > -1) {
//					comment = fileContents.substring(startCommentBlock, endCommentBlock + 2);
//					fileContents = fileContents.replace(comment, "");
//				} else {
//					endCommentBlock = fileContents.indexOf("\n", startCommentBlock);
//					if (endCommentBlock > -1) {
//						comment = fileContents.substring(startCommentBlock, endCommentBlock + 1);
//						fileContents = fileContents.replace(comment, "");
//					}
//				}
//			}
//			lastStart = startCommentBlock;
//			startCommentBlock = fileContents.indexOf("/*", startCommentBlock);
//			if (lastStart == startCommentBlock)
//				break;
//		}
//
//		int startCommentLine = fileContents.indexOf("//");
//		while (startCommentLine > -1) {
//			if (startCommentLine >= 0) {
//				int endCommentLine = fileContents.indexOf("\n", startCommentLine);
//				if (endCommentLine > -1) {
//					comment = fileContents.substring(startCommentLine, endCommentLine + 1);
//					fileContents = fileContents.replace(comment, "");
//				} else {
//					comment = fileContents.substring(startCommentLine, fileContents.length());
//					fileContents = fileContents.replace(comment, "");
//				}
//			}
//			lastStart = startCommentBlock;
//			startCommentLine = fileContents.indexOf("//", startCommentLine);
//			if (lastStart == startCommentBlock)
//				break;
//		}
		return _fileContents.trim().length() > 0;
	}

	protected void removeComments(String commentStart, String commentEnd) {
		String comment = "";
		int lastStart = -1;

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
			lastStart = startComment;
			startComment = _fileContents.indexOf(commentStart, startComment);
			if (lastStart == startComment)
				break;
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
			throw new RuntimeException(String.format("Error while trying to verify %s JAVA file has uncommented code lines.", filename), e);
		}

		return new String(buffer);
	}
}
