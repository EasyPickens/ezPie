using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.IO;
using System.Reflection;

namespace ScanManager.Common
{
    class MiscUtilities
    {
        public static String AppPath()
        {
            return Path.GetDirectoryName(new Uri(Assembly.GetExecutingAssembly().CodeBase).LocalPath);
        }

        public static String AppParentPath()
        {
            String path = AppPath();
            int firstIndex = path.IndexOf(Path.DirectorySeparatorChar);
            int lastIndex = path.LastIndexOf(Path.DirectorySeparatorChar);

            if (firstIndex != lastIndex)
                return path.Substring(0, lastIndex);

            return path;
        }

        public static String WrapSpaces(String value)
        {
            if ((value == null) || !value.Contains(" "))
                return value;

            return String.Format("\"{0}\"", value);
        }

        public static void VerifyDirectoryRights(String value)
        {
            // Check to see if it Exists
            if (!Directory.Exists(value))
                throw new Exception(String.Format("Requested directory {0} does not exist.", value));

            // Check to see if you can write to it
            if (!value.EndsWith(Path.DirectorySeparatorChar.ToString())) value += Path.DirectorySeparatorChar;

            String filename = value + "test.tmp";
            try
            {
                using (FileStream fstream = new FileStream(filename, FileMode.Create))
                {
                    using (TextWriter writer = new StreamWriter(fstream))
                    {
                        writer.WriteLine("Verify rights.");
                        writer.Close();
                    }
                    fstream.Close();
                }
                File.Delete(filename);
            }
            catch (Exception ex)
            {
                throw new Exception(String.Format("Do not have rights to create/write to files in {0}. " + ex.Message, value), ex);
            }
        }

        public static String LockFilename(String definitionName)
        {
            int pos = definitionName.LastIndexOf('.');
            if (pos != -1) definitionName = definitionName.Substring(0, pos);

            return String.Format("{0}{1}{2}.lck", ScanManager.Common.MiscUtilities.AppPath(), Path.DirectorySeparatorChar, definitionName);
        }

        public static Boolean isAlive(String definitionName)
        {
            try
            {
                String lockFile = LockFilename(definitionName);
                if (File.Exists(lockFile))
                {
                    File.Delete(lockFile);
                    // Delete worked, so thread is dead.
                    return false;
                }
                return true;
            }
            catch
            {
                // Delete failed, so file is still held by another thread.
                return true;
            }
        }

        public static int ObjectToInteger(Object value, String errorMessage)
        {
            if ((value == null) || (value.GetType().Name.Equals("DBNull")))
                throw new Exception(errorMessage);

            return (int)value;
        }

        public static String ObjectToString(Object value, String errorMessage)
        {
            if ((value == null) || (value.GetType().Name.Equals("DBNull")))
                throw new Exception(errorMessage);

            return (String)value;
        }

        public static String ObjectToStringDefault(Object value, String defaultValue)
        {
            if ((value == null) || (value.GetType().Name.Equals("DBNull")))
                return defaultValue;

            return (String)value;
        }
    }
}
