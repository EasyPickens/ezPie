/**
 *  
 * Copyright (c) 2017 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
 */

using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

using Microsoft.SharePoint.Client;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2017-06-05
 * 
 */

namespace PullSPData
{
    class Program
    {
        static int Main(string[] args)
        {
            if (args.Length < 2)
            {
                displayHelp();
                Console.WriteLine("Exit Code: 1");
                return 1;
            }

            // Parse the command line arguements
            string siteUrl = null; // = "http://sharepoint/sites/ProjectQualityRoom";
            string listName = null; // = "PQO Release Tracker";
            string viewName = null; // = "All Items-Ops Portal ETL";
            string saveFilename = null; // = @"C:\Developers\JUNK\Mytest.csv";
            string userName = null;
            string password = null;
            string camlQuery = null; // = "<View><Query><Where><Geq><FieldRef Name='Releases'/><Value Type='Number'>4700</Value></Geq></Where></Query><RowLimit>10</RowLimit></View>";

            string exeName = System.AppDomain.CurrentDomain.FriendlyName;
            exeName = exeName.Substring(0, exeName.Length - 4);

            Console.WriteLine(exeName);
            for (int i = 0; i < args.Length; i++)
            {
                // skip the odd index, it is the value.
                if (i % 2 != 0) continue;
                if (i + 1 >= args.Length) continue;
                switch (args[i].ToLower())
                {
                    case "-s":
                    case "-site":
                        siteUrl = args[i + 1];
                        break;
                    case "-l":
                    case "-list":
                        listName = args[i + 1];
                        break;
                    case "-v":
                    case "-view":
                        viewName = args[i + 1];
                        break;
                    case "-u":
                    case "-userid":
                        userName = args[i + 1];
                        break;
                    case "-p":
                    case "-password":
                        password = args[i + 1];
                        break;
                    case "-w":
                    case "-write":
                        saveFilename = args[i + 1];
                        break;
                    case "-q":
                    case "-caml_query":
                        camlQuery = args[i + 1];
                        break;
                    default:
                        Console.WriteLine("Exit Code: 2");
                        Console.WriteLine(String.Format("Invalid arguement: {0}: {1} is not a valid command line option.", args[i], args[i + 1]));
                        return 2;
                }
                Console.WriteLine(String.Format("   {0} {1}", args[i], args[i + 1]));
            }

            if (String.IsNullOrEmpty(saveFilename))
            {
                Console.WriteLine("Exit Code: 4");
                Console.WriteLine("ERROR: Missing required output filename.");
                return 4;
            }

            try
            {
                SharepointWrapper spw = new SharepointWrapper(siteUrl, listName, viewName);
                spw.Username = userName;
                spw.Password = password;
                spw.CamlQuery = camlQuery; // "<View><Query><Where><Geq><FieldRef Name='Releases'/><Value Type='Number'>4700</Value></Geq></Where></Query><RowLimit>10</RowLimit></View>";
                string savedFilename = spw.PullData(saveFilename);
                Console.WriteLine(String.Format("{0:n0} rows of data retrieved.", spw.RowCount));
                Console.WriteLine(String.Format("Data written to {0}", savedFilename));
                Console.WriteLine("Exit Code: 0");
                return 0;
            }
            catch (Exception ex)
            {
                Console.WriteLine("Exit Code: 3");
                Console.WriteLine(String.Format("Error while pulling data. {0}", ex.Message));
                return 3;
            }
        }

        public static int displayHelp()
        {
            string exeName = System.AppDomain.CurrentDomain.FriendlyName;
            exeName = exeName.Substring(0, exeName.Length - 4);
            Console.WriteLine(exeName);
            Console.WriteLine("  Downloads data from the specified Sharepoint site and saves it as a ");
            Console.WriteLine("  delimited file (CSV). Data requested can be Lists avalable, Views ");
            Console.WriteLine("  available or items in a View. If a user name and password are not ");
            Console.WriteLine("  provided the program uses the default credentials of the current ");
            Console.WriteLine("  process identity.");
            Console.WriteLine();
            Console.WriteLine("Command Line Options: ");
            Console.WriteLine("  -w, -write    = The path and name of the output file.");
            Console.WriteLine("  -s, -site     = the url to the Sharepoint site ");
            Console.WriteLine("  -l, -list     = Name of the list (optional) ");
            Console.WriteLine("  -v, -view     = Name of the view (optional) ");
            Console.WriteLine("  -u, -userid   = User ID to make the request (optional) ");
            Console.WriteLine("  -p, -password = Password to make the request (optional) ");
            Console.WriteLine("  -q, -query    = CAML query (optional) ");
            Console.WriteLine();
            Console.WriteLine("Example that returns avaliable lists: ");
            Console.WriteLine(String.Format("  {0} -s http://host/site/mysp -w availableLists.csv ", exeName));
            Console.WriteLine();
            Console.WriteLine("Example that returns avaliable views: ");
            Console.WriteLine(String.Format("  {0} -s http://host/site/mysp -l \"my list\" -w viewlist.csv ", exeName));
            Console.WriteLine();
            Console.WriteLine("Example that returns view items: ");
            Console.WriteLine(String.Format("  {0} -s http://host/site/mysp -l \"my list\" -v \"my view\" -w items.csv ", exeName));
            Console.WriteLine();
            return 1;
        }

        public static void WriteTextFile(string Filename, string columnNames, List<string> data)
        {
            using (System.IO.StreamWriter file = new System.IO.StreamWriter(Filename))
            {
                if (!String.IsNullOrEmpty(columnNames))
                    file.WriteLine(columnNames);

                foreach (string line in data)
                {
                    file.WriteLine(line);
                }
            }
        }

        private static void querySharepoint()
        {
            string siteUrl = "http://sharepoint/sites/ProjectQualityRoom";
            string ListName = "PQO Release Tracker";
            //string ViewName = "rickstestview"; // "All Items";
            string ViewName = "All Items-Ops Portal ETL";

            ClientContext context = new ClientContext(siteUrl);
            List list = context.Web.Lists.GetByTitle(ListName);
            context.Load(list);
            context.ExecuteQuery();

            View view = list.Views.GetByTitle(ViewName);
            context.Load(view);
            context.ExecuteQuery();
            CamlQuery query = new CamlQuery();
            query.ViewXml = view.ViewQuery;
            Console.WriteLine(view.ViewQuery);
            query.ViewXml = "<View><Query><Where><Geq><FieldRef Name='Releases'/><Value Type='Number'>4700</Value></Geq></Where></Query><RowLimit>10</RowLimit></View>";

            ListItemCollection items = list.GetItems(query);
            context.Load(items);
            context.ExecuteQuery();
            Console.WriteLine(items.Count);

            int rowNumber = 0;
            List<string> keys = new List<string>();
            StringBuilder header = new StringBuilder();
            List<string> fieldNames = new List<string>();
            List<string> contents = new List<string>();
            foreach (ListItem item in items)
            {
                int col = 0;
                if (rowNumber == 0)
                {
                    // Read the keys from the first row only
                    foreach (KeyValuePair<string, object> kvp in item.FieldValues)
                    {
                        if (col > 0) header.Append(",");
                        header.Append(cleanupColumnName(kvp.Key));
                        fieldNames.Add(cleanupColumnName(kvp.Key));
                        keys.Add(kvp.Key);
                        col++;
                    }
                    WriteTextFile(@"C:\Developers\JUNK\PQOReleaseTracker_Columns.csv", "", fieldNames);
                    col = 0;
                }

                StringBuilder row = new StringBuilder();
                for (int i = 0; i < keys.Count; i++)
                {
                    if (col > 0) row.Append(",");
                    Object value = "";
                    if (item.FieldValues.ContainsKey(keys[i]))
                    {
                        value = item.FieldValues[keys[i]];
                        if (value == null)
                            value = "";
                        else if (value.GetType().Name.Equals("FieldUserValue"))
                        {
                            FieldUserValue fieldUserValue = (Microsoft.SharePoint.Client.FieldUserValue)item.FieldValues[keys[i]];
                            value = fieldUserValue.LookupValue;
                        }
                    }
                    row.Append(wrap(value.ToString()));
                    col++;
                }
                rowNumber++;
                contents.Add(row.ToString());
            }
            WriteTextFile(@"C:\Developers\JUNK\PQOReleaseTracker_test2.csv", header.ToString(), contents);
        }

        private static string wrap(string value)
        {
            if (String.IsNullOrEmpty(value))
                return value;

            bool addQuotes = false;
            if (value.Contains("\""))
            {
                value = value.Replace("\"", "\"\"");
                addQuotes = true;
            }
            if (value.Contains(","))
            {
                addQuotes = true;
            }

            if (addQuotes)
                return "\"" + value + "\"";
            else
                return value;
        }

        private static string cleanupColumnName(string name)
        {
            // Not very effecient, but sharepoint returns some crazy column names 
            String newName = System.Xml.XmlConvert.DecodeName(name);
            newName = newName.Replace("_x0020", " ");
            newName = newName.Replace("_x003f", " ");
            newName = newName.Replace("_x002", " ");
            newName = newName.Replace("_x00", " ");

            bool wasSpace = false;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < newName.Length; i++)
            {
                if ("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".IndexOf(newName[i]) > -1)
                {
                    sb.Append(newName[i]);
                    wasSpace = false;
                }
                else if (" /&.".IndexOf(newName[i]) > -1)
                {
                    if (!wasSpace) sb.Append(' ');
                    wasSpace = true;
                }
            }
            newName = sb.ToString().Trim();
            newName = newName.Replace(" ", "_");

            return newName;
        }
    }
}
