package com.fanniemae.automation.common;

import java.io.File;
import java.util.UUID;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-15
 * 
 */
public class FileUtilities {

	public static boolean isValidDirectory(String filePath) {
		File f = new File(filePath);
		return f.exists() && f.isDirectory();
	}

	public static boolean isInvalidDirectory(String filePath) {
		return !isValidDirectory(filePath);
	}

	public static boolean isValidFile(String fileName) {
		File f = new File(fileName);
		return f.exists() && f.isFile();
	}

	public static boolean isInvalidFile(String fileName) {
		return !isValidFile(fileName);
	}

	public static String formatPath(String path, String defaultPath, String attributeName) {
		if (StringUtilities.isNullOrEmpty(path))
			path = defaultPath;
		else if (FileUtilities.isInvalidDirectory(path))
			throw new RuntimeException(String.format("The directory value assigned to %s does not exist.", attributeName));

		if (!path.endsWith(File.separator))
			path = path + File.separator;
		return path;
	}

	public static String getRandomFilename(String filePath) {
		String sRandomGuid = UUID.randomUUID().toString().replace("-", "");
		String sDirectory = filePath;
		if (!sDirectory.endsWith(File.separator)) {
			sDirectory += File.separator;
		}
		return String.format("%s%s.tmp", sDirectory, sRandomGuid);
	}

	public static String getDataFilename(String filePath, String datasetXML, String connectionXML) {
		String sIdentifier = datasetXML;
		if (connectionXML != null) {
			sIdentifier = sIdentifier + "|" + connectionXML;
		}
		return getHashFilename(filePath, sIdentifier, "dat");
	}

	public static String getFilenameWithoutExtension(String fileName) {
		File f = new File(fileName);
		String sName = f.getName();
		if (StringUtilities.isNotNullOrEmpty(sName) && (sName.indexOf(".") > 0)) {
			return sName.substring(0, sName.indexOf("."));
		}
		return sName;
	}

	private static String getHashFilename(String filePath, String datasetXML, String fileExtension) {
		String sFilename = CryptoUtilities.hashValue(datasetXML);
		return String.format("%s%s.%s", filePath, sFilename, fileExtension);
	}
}
