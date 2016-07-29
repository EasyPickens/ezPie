package com.fanniemae.devtools.pie.common;

import java.io.File;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
	
	public static String[] zip(String directory, String zipFilename, Pattern[] includeFileRegex, Pattern[] excludeFileRegex) throws IOException {
		File sourceDir = new File(directory);
		File zipFile = new File(zipFilename);
		return zip(sourceDir, zipFile, includeFileRegex, excludeFileRegex, null, null);
	}
	
	public static String[] zip(String directory, String zipFilename, Pattern[] includeFileRegex, Pattern[] excludeFileRegex, Pattern[] includeDirectoryRegex, Pattern[] excludeDirectoryRegex) throws IOException {
		File sourceDir = new File(directory);
		File zipFile = new File(zipFilename);
		return zip(sourceDir, zipFile, includeFileRegex, excludeFileRegex, includeDirectoryRegex, excludeDirectoryRegex);
	}

	public static String[] zip(File directory, File zipfile, Pattern[] includeFileRegex, Pattern[] excludeFileRegex, Pattern[] includeDirectoryRegex, Pattern[] excludeDirectoryRegex) throws IOException {
		List<String> filelist = new ArrayList<String>();
		filelist.add(TABLE_HEADER);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		long totalSize = 0L;
		
		boolean hasIncludeFileFilter = (includeFileRegex != null) ? true : false;
		boolean hasExcludeFileFilter = (excludeFileRegex != null) ? true : false;
		boolean hasIncludeDirectoryFilter = (includeDirectoryRegex != null) ? true : false;
		boolean hasExcludeDirectoryFilter = (excludeDirectoryRegex != null) ? true : false;
		
		URI base = directory.toURI();
		Deque<Entry> queue = new LinkedList<Entry>();
		queue.push(new Entry(directory, false));
		Entry entry;
		boolean excluded = false;
		
		try (OutputStream out = new FileOutputStream(zipfile, false); ZipOutputStream zout = new ZipOutputStream(out)) {
			int row = 0;
			ZipEntry ze;
			while (!queue.isEmpty()) {
				entry = queue.pop();
				directory = entry._file;
				excluded = entry._excluded;
				for (File file : directory.listFiles()) {
					String name = base.relativize(file.toURI()).getPath();
					if (file.isDirectory()) {
						if (hasIncludeDirectoryFilter && matchesRegexFilter(name, includeDirectoryRegex)) {
							// include filters override exclude filters
						} else if (hasExcludeDirectoryFilter && matchesRegexFilter(name, excludeDirectoryRegex)) {
							continue;
						}
						name = name.endsWith("/") ? name : name + "/";
						ze = new ZipEntry(name);
						ze.setTime(file.lastModified());
						zout.putNextEntry(ze);
						queue.push(new Entry(file, false));
						
					} else {
						if (hasIncludeFileFilter && matchesRegexFilter(name, includeFileRegex)) {
							// include filters override exclude filters
						} else if ((hasExcludeFileFilter && matchesRegexFilter(name, excludeFileRegex)) || excluded) {
							continue;
						}
						
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
		return unzip(zipFilename, directory, null, null);
	}

	public static String[] unzip(String zipFilename, String directory, Pattern[] includeFileRegex, Pattern[] excludeFileRegex) throws IOException {
		File zipFile = new File(zipFilename);
		File destDirectory = new File(directory);
		return unzip(zipFile, destDirectory, includeFileRegex, excludeFileRegex);
	}

	public static String[] unzip(File zipfile, File directory, Pattern[] includeFileRegex, Pattern[] excludeFileRegex) throws IOException {
		List<String> filelist = new ArrayList<String>();
		filelist.add(TABLE_HEADER);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		long totalSize = 0L;
		
		boolean hasIncludeFileFilter = (includeFileRegex != null) ? true : false;
		boolean hasExcludeFileFilter = (excludeFileRegex != null) ? true : false;
		
		try (ZipFile zfile = new ZipFile(zipfile)) {
			Enumeration<? extends ZipEntry> entries = zfile.entries();
			int row = 0;
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				
				File file = new File(directory, entry.getName());
				
				if (entry.isDirectory()) {
					file.mkdirs();
					file.setLastModified(entry.getTime());
					
				} else {
					
					if (hasIncludeFileFilter && matchesRegexFilter(entry.getName(), includeFileRegex)) {
						// include filters override exclude filters
					} else if (hasExcludeFileFilter && matchesRegexFilter(entry.getName(), excludeFileRegex)) {
						continue;
					}
					
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
	
	protected static boolean matchesRegexFilter(String value, Pattern[] regexFilter) {
		if (regexFilter == null)
			return false;

		for (int x = 0; x < regexFilter.length; x++) {
			Matcher m = regexFilter[x].matcher(value);
			if (m.find()) {
				return true;
			}
		}
		return false;
	}
	
	protected static class Entry {
		protected File _file;
		protected boolean _excluded;
		
		protected Entry(File file, boolean excluded){
			_file = file;
			_excluded = excluded;
		}
	}
	
}
