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

//<Move 
//   Source 
//   Destination 
//   [IncludeFiles] default all
//   [ExcludeFiles] default none
//   [IncludeDirectories] default all
//   [ExcludeDirectories] default none
//   [ClearReadOnly] default false
//   [SkipHidden] default false
//   [Shallow] default true
///>

public class Move extends Copy {

	public Move(SessionManager session, Element action) {
		super(session, action);
		_copyOptions = new CopyOption[] { StandardCopyOption.REPLACE_EXISTING };
		_countMessage = "moved";
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
			Files.move(sourcePath, destinationPath, _copyOptions);
			_filesProcessed++;
		} catch (IOException e) {
			RuntimeException ex = new RuntimeException(String.format("Error while trying to move %s to %s. %s", source, destFilename, e.getMessage()), e);
			_session.addErrorMessage(ex);
			throw ex;
		}
	}

	@Override
	protected void postprocessDirectory(String source) {
		// remove the empty directories
		if (FileUtilities.isEmptyDirectory(source)) {
			try {
				File dir = new File(source);
				dir.delete();
			} catch (Exception e) {
				RuntimeException ex = new RuntimeException(String.format("Could not remove empty directory (%s) during move operation. %s", source, e.getMessage()), e);
				throw ex;
			}
		}
	}

}
