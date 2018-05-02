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

package com.fanniemae.ezpie.common;

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

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @author Tara Tritt
 * @since 2016-05-06
 * 
 */

public final class ZipUtilities {

	protected static String TABLE_HEADER = "<table><tr><td>&nbsp;</td><td>Filename</td><td>Last Modified</td><td>Size</td></tr>";
	protected static String TABLE_ROW = "<tr><td style=\"text-align: right;\">%,d</td><td>%s</td><td>%s</td><td style=\"text-align: right;\">%,d bytes</td></tr>";
	protected static String TABLE_FOOTER = "<tr><td colspan=\"3\" style=\"text-align: right;\">Total Size:</td><td style=\"text-align: right;\">%,d bytes</td></tr></table>";

	private ZipUtilities() {
	}
	
	public static String[] zip(String directory, String zipFilename) throws IOException {
		return zip(directory, zipFilename, null, null);
	}
	
	public static String[] zip(String directory, String zipFilename, Pattern[] includeRegex, Pattern[] excludeRegex) throws IOException {
		File sourceDir = new File(directory);
		File zipFile = new File(zipFilename);
		return zip(sourceDir, zipFile, includeRegex, excludeRegex);
	}

	public static String[] zip(File directory, File zipfile, Pattern[] includeRegex, Pattern[] excludeRegex) throws IOException {
		List<String> filelist = new ArrayList<String>();
		filelist.add(TABLE_HEADER);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		long totalSize = 0L;
		
		boolean hasIncludeFilter = (includeRegex != null && includeRegex.length != 0) ? true : false;
		boolean hasExcludeFilter = (excludeRegex != null && excludeRegex.length != 0) ? true : false;
		
		URI base = directory.toURI();
		Deque<Entry> queue = new LinkedList<Entry>();
		queue.push(new Entry(directory, !hasIncludeFilter));
		Entry entry;
		boolean included = true;
		
		try (OutputStream out = new FileOutputStream(zipfile, false); ZipOutputStream zout = new ZipOutputStream(out)) {
			int row = 0;
			ZipEntry ze;
			while (!queue.isEmpty()) {
				entry = queue.pop();
				directory = entry._file;
				included = entry._included;
				for (File file : directory.listFiles()) {
					String path = base.relativize(file.toURI()).getPath();
					String name = file.getName();
					if (file.isDirectory()) {
						if (hasIncludeFilter && matchesRegexFilter(name, includeRegex) || included){
							
						} else if (hasIncludeFilter && !matchesRegexFilter(name, includeRegex)) {
							queue.push(new Entry(file, false));
							continue;
						} else if (hasExcludeFilter && matchesRegexFilter(name, excludeRegex)){
							continue;
						}
						
						queue.push(new Entry(file, true));
						
						name = name.endsWith("/") ? name : name + "/";
						ze = new ZipEntry(path);
						ze.setTime(file.lastModified());
						zout.putNextEntry(ze);
						
					} else {
						if (hasIncludeFilter && matchesRegexFilter(name, includeRegex)) {
							
						} else if (hasIncludeFilter && !matchesRegexFilter(name, includeRegex) && !included){
							continue;
						} else if (hasExcludeFilter && matchesRegexFilter(name, excludeRegex)){
							continue;
						}
						
						ze = new ZipEntry(path);
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

	public static String[] unzip(String zipFilename, String directory, Pattern[] includeRegex, Pattern[] excludeRegex) throws IOException {
		File zipFile = new File(zipFilename);
		File destDirectory = new File(directory);
		return unzip(zipFile, destDirectory, includeRegex, excludeRegex);
	}

	public static String[] unzip(File zipfile, File directory, Pattern[] includeRegex, Pattern[] excludeRegex) throws IOException {
		List<String> filelist = new ArrayList<String>();
		filelist.add(TABLE_HEADER);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		long totalSize = 0L;
		
		boolean hasIncludeFilter = (includeRegex != null && includeRegex.length != 0) ? true : false;
		boolean hasExcludeFilter = (excludeRegex != null && excludeRegex.length != 0) ? true : false;
		
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
					String name = entry.getName();
					if (hasIncludeFilter && matchesRegexFilter(name, includeRegex)) {
						// include filters override exclude filters
					} else if (hasIncludeFilter && !matchesRegexFilter(name, includeRegex)) {
						continue;
					} else if (hasExcludeFilter && matchesRegexFilter(name, excludeRegex)) {
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
					filelist.add(String.format(TABLE_ROW, row, name, sdf.format(entry.getTime()), size));
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
		protected boolean _included;
		
		protected Entry(File file, boolean included){
			_file = file;
			_included = included;
		}
	}
	
}
