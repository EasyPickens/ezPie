using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.IO;

namespace ScanManager.Common
{
    public class LogCleanUp
    {
        protected String _directory;
        protected String _extension;
        protected List<String> _validFiles = new List<String>();
        protected List<String> _deleteFiles = new List<String>();
        //protected StringBuilder _report = new StringBuilder();

        public LogCleanUp(String directory, String extension)
        {
            _directory = directory;
            _extension = extension;
        }

        public String findOrphanFiles()
        {
            DirectoryInfo folder = new DirectoryInfo(_directory);
            FileInfo[] listOfFiles = folder.GetFiles("*" + _extension);

            int totalCount = 0;
            // first read the contents of all the 'application' HTML files on the drive to build list of valid log files
            for (int i = 0; i < listOfFiles.Length; i++)
            {
                String name = listOfFiles[i].Name;
                if (listOfFiles[i].CreationTime >= DateTime.Now.AddHours(-4))
                    continue;
                else if (name.EndsWith(_extension, StringComparison.CurrentCultureIgnoreCase) && (name.Length == 37) && isValidGUID(name))
                    continue;
                else if (name.EndsWith(_extension, StringComparison.CurrentCultureIgnoreCase))
                    parseHtml(listOfFiles[i].FullName);
            }

            // Now check GUID HTML files - parsing only those found in the 'good' list.
            // These files are usually related to database backups or CAST CED deployment.
            for (int i = 0; i < listOfFiles.Length; i++)
            {
                String name = listOfFiles[i].Name;
                if (listOfFiles[i].CreationTime >= DateTime.Now.AddHours(-2))
                    continue;
                else if (name.EndsWith(_extension, StringComparison.CurrentCultureIgnoreCase) && (name.Length == 37) && isValidGUID(name))
                {
                    if (!_validFiles.Contains(name))
                    {
                        totalCount++;
                        //_report.AppendLine(String.Format("del {0}", listOfFiles[i].FullName));
                         File.Delete(listOfFiles[i].FullName);
                    }
                    else
                    {
                        // Add any referenced files to the list of valid files.
                        parseHtml(listOfFiles[i].FullName);
                    }
                }
            }

            // Compare valid list to full directory contents.
            listOfFiles = folder.GetFiles();
            for (int i = 0; i < listOfFiles.Length; i++)
            {
                String name = listOfFiles[i].Name;
                if (name.EndsWith(".html", StringComparison.CurrentCultureIgnoreCase))
                {
                    // Skip the HTML files because they have been processed already.
                    continue;
                }
                else if (!_validFiles.Contains(name))
                {
                    totalCount++;
                    //_report.AppendLine(String.Format("del {0}", listOfFiles[i].FullName));
                    File.Delete(listOfFiles[i].FullName);
                }
            }
            //_report.AppendLine(String.Format("Number of files to delete: {0:n0}", totalCount));
            //return _report.ToString();
            return String.Format("Number of files deleted: {0:n0}", totalCount);
        }

        protected void parseHtml(String htmlFilename)
        {
            String contents = loadFile(htmlFilename);

            int startPos = 0;
            int end = -1;
            int start = contents.IndexOf("<a href=\"", startPos);
            while (start > 0)
            {
                end = contents.IndexOf('"', start + 9);
                String bb = contents.Substring(start + 9, end - start - 9);
                _validFiles.Add(contents.Substring(start + 9, end - start - 9));
                startPos = end + 1;
                start = contents.IndexOf("<a href=\"", startPos);
            }
        }

        protected String loadFile(String filename)
        {
            StringBuilder sb = new StringBuilder();
            try
            {
                using (StreamReader sr = new StreamReader(filename))
                {
                    while (!sr.EndOfStream)
                    {
                        sb.AppendLine(sr.ReadLine());
                    }
                    sr.Close();
                }
            }
            catch (Exception ex)
            {
                throw new Exception(String.Format("Error while trying to read {0} text file. {1}", filename, ex.Message), ex);
            }
            return sb.ToString();
        }

        protected Boolean isValidGUID(String value)
        {
            int pos = value.IndexOf('.');
            if (pos != -1)
            {
                value = value.Substring(0, pos);
            }
            if (value.Length != 32)
            {
                return false;
            }
            char[] notGuidChars = { ' ', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '_' };
            int length = notGuidChars.Length;
            for (int i = 0; i < length; i++)
            {
                if (value.IndexOf(notGuidChars[i]) != -1)
                {
                    return false;
                }
            }
            return true;
        }
    }
}
