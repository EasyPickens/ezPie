package com.fanniemae.devtools.pie.common;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ZipUtilities {

	protected static String TABLE_HEADER = "<table><tr><td>&nbsp;</td><td>Filename</td><td>Last Modified</td><td>Size</td></tr>";
	protected static String TABLE_ROW = "<tr><td style=\"text-align: right;\">%,d</td><td>%s</td><td>%s</td><td style=\"text-align: right;\">%,d bytes</td></tr>";
	protected static String TABLE_FOOTER = "<tr><td colspan=\"3\" style=\"text-align: right;\">Total Size:</td><td style=\"text-align: right;\">%,d bytes</td></tr></table>";

	public static String[] zip(String directory, String zipFilename) throws IOException {
		return zip(directory, zipFilename, null, null, null, null);
	}
	
	public static String[] zip(String directory, String zipFilename,  FileFilter includeFiles) throws IOException {
		return zip(directory, zipFilename, includeFiles, null, null, null);
	}
	
	public static String[] zip(String directory, String zipFilename, FileFilter includeFiles, FileFilter excludeFiles, FileFilter includeDirectories, FileFilter excludeDirectories) throws IOException {
		File sourceDir = new File(directory);
		File zipFile = new File(zipFilename);
		return zip(sourceDir, zipFile, includeFiles, excludeFiles, includeDirectories, excludeDirectories);
	}

	public static String[] zip(File directory, File zipfile, FileFilter includeFilesFilter, FileFilter excludeFilesFilter, FileFilter includeDirectoriesFilter, FileFilter excludeDirectoriesFilter) throws IOException {
		List<String> filelist = new ArrayList<String>();
		filelist.add(TABLE_HEADER);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		long totalSize = 0L;
		
		boolean hasIncludeFileFilter = (includeFilesFilter != null) ? true : false;
		boolean hasExcludeFileFilter = (excludeFilesFilter != null) ? true : false;
		boolean hasIncludeDirectoryFilter = (includeDirectoriesFilter != null) ? true : false;
		boolean hasExcludeDirectoryFilter = (excludeDirectoriesFilter != null) ? true : false;
		
		URI base = directory.toURI();
		Deque<File> queue = new LinkedList<File>();
		queue.push(directory);
		
		try (OutputStream out = new FileOutputStream(zipfile, false); ZipOutputStream zout = new ZipOutputStream(out)) {
			int row = 0;
			ZipEntry ze;
			while (!queue.isEmpty()) {
				directory = queue.pop();
				File[] includeFiles = directory.listFiles(includeFilesFilter);
				File[] excludeFiles = (hasExcludeFileFilter) ? directory.listFiles(excludeFilesFilter) : null;
				File[] includeDirectories = directory.listFiles(includeDirectoriesFilter);
				File[] excludeDirectories = (hasExcludeDirectoryFilter) ? directory.listFiles(excludeDirectoriesFilter) : null;
				for (File file : directory.listFiles()) {
					String name = base.relativize(file.toURI()).getPath();
					if (file.isDirectory()) {
						if((!hasExcludeDirectoryFilter && !hasIncludeDirectoryFilter && 
								!hasExcludeFileFilter && !hasIncludeFileFilter) || 
								(hasIncludeDirectoryFilter && inArray(file.getName(), includeDirectories)) ||
								(hasExcludeDirectoryFilter && !inArray(file.getName(), excludeDirectories))){
							name = name.endsWith("/") ? name : name + "/";
							ze = new ZipEntry(name);
							ze.setTime(file.lastModified());
							zout.putNextEntry(ze);
						} 
						
						if (hasExcludeDirectoryFilter && inArray(file.getName(), excludeDirectories)) {
							continue;
						} else if (hasIncludeDirectoryFilter && !inArray(file.getName(), includeDirectories)) {
							continue;
						} 
						queue.push(file);
					
					} else {
						if (hasExcludeFileFilter && inArray(file.getName(), excludeFiles)) 
							continue;
						else if (hasIncludeFileFilter && !inArray(file.getName(), includeFiles)) 
							continue;
						
						ze = new ZipEntry(name);
						ze.setTime(file.lastModified());
						zout.putNextEntry(ze);
						copy(file, zout);
						zout.closeEntry();

						long size = file.length();
						row++;
						filelist.add(String.format(TABLE_ROW, row, name, sdf.format(file.lastModified()), size));
						totalSize += size;
					}
				}
			}
			zout.close();
			out.close();
		}
		filelist.add(String.format(TABLE_FOOTER, totalSize));
		return filelist.toArray(new String[filelist.size()]);
	}
	
	public static String[] unzip(String zipFilename, String directory) throws IOException {
		return unzip(zipFilename, directory, null, null, null, null);
	}
	
	public static String[] unzip(String zipFilename, String directory,  FileFilter includeFiles) throws IOException {
		return unzip(zipFilename, directory, includeFiles, null, null, null);
	}

	public static String[] unzip(String zipFilename, String directory, FileFilter includeFiles, FileFilter excludeFiles, FileFilter includeDirectories, FileFilter excludeDirectories) throws IOException {
		File zipFile = new File(zipFilename);
		File destDirectory = new File(directory);
		return unzip(zipFile, destDirectory, includeFiles, excludeFiles, includeDirectories, excludeDirectories);
	}

	public static String[] unzip(File zipfile, File directory, FileFilter includeFilesFilter, FileFilter excludeFilesFilter, FileFilter includeDirectoriesFilter, FileFilter excludeDirectoriesFilter) throws IOException {
		List<String> filelist = new ArrayList<String>();
		filelist.add(TABLE_HEADER);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		long totalSize = 0L;
		
		boolean hasIncludeFileFilter = (includeFilesFilter != null) ? true : false;
		boolean hasExcludeFileFilter = (excludeFilesFilter != null) ? true : false;
		
		try (ZipFile zfile = new ZipFile(zipfile)) {
			Enumeration<? extends ZipEntry> entries = zfile.entries();
			int row = 0;
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				
				File file = new File(directory, entry.getName());
				boolean includeFile = (hasIncludeFileFilter) ? includeFilesFilter.accept(file) : false;
				boolean excludeFile = (hasExcludeFileFilter) ? excludeFilesFilter.accept(file): false;
				
				if (entry.isDirectory()) {
					file.mkdirs();
					file.setLastModified(entry.getTime());
					
				} else {
					
					if (hasExcludeFileFilter && excludeFile) 
						continue;
					else if (hasIncludeFileFilter && !includeFile) 
						continue;
					
					file.getParentFile().mkdirs();

					try (InputStream in = zfile.getInputStream(entry)) {
						copy(in, file);
						in.close();
					}
					file.setLastModified(entry.getTime());
					long size = entry.getSize();
					totalSize += size;
					row++;
					filelist.add(String.format(TABLE_ROW, row, entry.getName(), sdf.format(entry.getTime()), size));
				}
			}
			zfile.close();
		}
		filelist.add(String.format(TABLE_FOOTER, totalSize));
		return filelist.toArray(new String[filelist.size()]);
	}

	private static void copy(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[4096];
		while (true) {
			int readCount = in.read(buffer);
			if (readCount < 0) {
				break;
			}
			out.write(buffer, 0, readCount);
		}
	}

	private static void copy(File file, OutputStream out) throws IOException {
		try (InputStream in = new FileInputStream(file)) {
			copy(in, out);
			in.close();
		}
	}

	private static void copy(InputStream in, File file) throws IOException {
		try (OutputStream out = new FileOutputStream(file)) {
			copy(in, out);
			out.close();
		}
	}
	
	protected static boolean inArray(String value, File[] list) {
		if (list == null)
			return false;

		boolean found = false;
		for (int x = 0; x < list.length; x++) {
			if (list[x].getName().equals(value)) {
				found = true;
				break;
			}
		}
		return found;
	}
	
}
