package com.fanniemae.devtools.pie.common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

import org.w3c.dom.Element;

/**
 * 
 * @author Richard Monson
 * @since 2015-12-15
 * 
 */
public class FileUtilities {

	public static boolean isValidDirectory(String filePath) {
		if (filePath == null) return false;
		File f = new File(filePath);
		return f.exists() && f.isDirectory();
	}

	public static boolean isInvalidDirectory(String filePath) {
		return !isValidDirectory(filePath);
	}

	public static boolean isEmptyDirectory(String filePath) {
		if (filePath == null) return false;
		File f = new File(filePath);
		return f.exists() && f.isDirectory() & (f.list().length == 0);
	}

	public static boolean isNotEmptyDirectory(String filePath) {
		return !isEmptyDirectory(filePath);
	}

	public static boolean isValidFile(String fileName) {
		if (fileName == null) return false;
		File f = new File(fileName);
		return f.exists() && f.isFile();
	}

	public static long getLength(String fileName) {
		if (fileName == null) return 0L;
		File f = new File(fileName);
		return f.exists() ? f.length() : 0L;
	}

	public static boolean isInvalidFile(String fileName) {
		return !isValidFile(fileName);
	}

	public static boolean deleteFile(String fileName) {
		if (StringUtilities.isNullOrEmpty(fileName)) {
			return true;
		}

		File fi = new File(fileName);
		if (!fi.exists()) {
			return true;
		}

		return fi.delete();
	}

	public static String addDirectory(String path, String directory) {
		if (!path.endsWith(File.separator))
			path = path + File.separator;

		if (directory.startsWith(File.separator)) {
			directory = directory.substring(1);
		}
		return path + directory;
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
		return getRandomFilename(filePath, "tmp");
	}

	public static String getRandomFilename(String filePath, String extension) {
		String sRandomGuid = UUID.randomUUID().toString().replace("-", "");
		String sDirectory = filePath;
		if (!sDirectory.endsWith(File.separator)) {
			sDirectory += File.separator;
		}
		return String.format("%s%s.%s", sDirectory, sRandomGuid, extension);
	}

	public static String getDataFilename(String filepath, Element eleDataset, Element eleConnection) {
		return getDataFilename(filepath, XmlUtilities.getOuterXml(eleDataset), XmlUtilities.getOuterXml(eleConnection));
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

	public static String loadFile(String filename) {
		if (!FileUtilities.isValidFile(filename))
			throw new RuntimeException(String.format("%s file not found.", filename));

		try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
			StringBuilder sb = new StringBuilder();
			String sLine = br.readLine();
			boolean bAddNewLine = false;
			while (sLine != null) {
				if (bAddNewLine)
					sb.append("\n");
				sb.append(sLine);
				sLine = br.readLine();
				bAddNewLine = true;
			}
			return sb.toString();
		} catch (IOException e) {
			throw new RuntimeException(String.format("Error while trying to read %s text file.", filename), e);
		}
	}

	public static String writeRandomTextFile(String path, String contents) {
		return writeRandomFile(path, "txt", contents);
	}

	public static Boolean isGitRepository(String path) {
		if (path == null)
			return false;

		String repoPath = !path.endsWith(File.separator) ? path + File.separator : path;
		return isValidDirectory(repoPath+".git");
	}
	
	public static Boolean isSvnRepository(String path) {
		if (path == null)
			return false;

		String repoPath = !path.endsWith(File.separator) ? path + File.separator : path;
		return isValidDirectory(repoPath+".svn");
	}

	public static String writeRandomFile(String path, String extension, String contents) {
		String sFilename = getRandomFilename(path, extension);
		try (FileWriter fw = new FileWriter(sFilename); BufferedWriter bw = new BufferedWriter(fw);) {
			bw.write(contents);
			bw.close();
			fw.close();
		} catch (IOException e) {
			throw new RuntimeException(String.format("Error while trying to write text file to %s", path), e);
		}
		return sFilename;
	}

	private static String getHashFilename(String filePath, String datasetXML, String fileExtension) {
		String sFilename = CryptoUtilities.hashValue(datasetXML);
		return String.format("%s%s.%s", filePath, sFilename, fileExtension);
	}
}
