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

// <Copy 
//		Source 
//		Destination 
//		[IncludeFiles] default all
//		[ExcludeFiles] default none
//      [IncludeDirectories] default all
//      [ExcludeDirectories] default none
//      [ClearReadOnly] default false
//      [SkipHidden] default false
//		[Shallow] default true
// />

public class Copy extends FileSystemAction {

	protected CopyOption[] _copyOptions = new CopyOption[] { StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES };

	public Copy(SessionManager session, Element action) {
		super(session, action);

		_source = requiredAttribute("Source", String.format("%s action requires a source directory or file.", _actionName));
		_isFile = FileUtilities.isValidFile(_source);
		_isDirectory = FileUtilities.isValidDirectory(_source);
		if (!_isFile && !_isDirectory) {
			throw new RuntimeException(String.format("%s action requires a source value to an existing directory or file. %s is not a valid file or directory.", _actionName, _source));
		}
		_type = (_isFile) ? "file" : "directory";
		_destination = requiredAttribute("Destination", String.format("%s action requires a destination %s.", _actionName, _type));
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
