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
using System.IO;
using System.Net;
using System.Xml;

using Microsoft.SharePoint;
using Microsoft.SharePoint.Client;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2017-06-05
 * 
 */

namespace PullSPData
{
    class SharepointWrapper
    {
        private string _siteUrl;
        private string _listName;
        private string _viewName;
        private string _camlQuery;
        private string _username;
        private string _password;

        private string _saveFilename;
        private string _delimiter = ",";

        private List<string> _columnNames;
        private List<Dictionary<string, string>> _dataRows = null;

        private int _rowcount = 0;

        public string Delimiter
        {
            get { return _delimiter; }
            set { _delimiter = value; }
        }

        public string Username
        {
            get { return _username; }
            set { _username = value; }
        }

        public string Password
        {
            get { return _password; }
            set { _password = value; }
        }

        public string SaveFilename
        {
            get { return _saveFilename; }
            set { _saveFilename = value; }
        }

        public string CamlQuery
        {
            get { return _camlQuery; }
            set { _camlQuery = value; }
        }

        public int RowCount
        {
            get { return _rowcount; }
        }

        public SharepointWrapper(String siteUrl)
            : this(siteUrl, null, null)
        { }

        public SharepointWrapper(String siteUrl, String listName)
            : this(siteUrl, listName, null)
        { }

        public SharepointWrapper(String siteUrl, String listName, String viewName)
        {
            _siteUrl = siteUrl;
            _listName = listName;
            _viewName = viewName;
            _dataRows = new List<Dictionary<string, string>>();
        }

        public string PullData()
        {
            return PullData(_saveFilename);
        }

        public string PullData(string saveFilename)
        {
            _saveFilename = saveFilename;
            if (String.IsNullOrEmpty(_saveFilename))
                throw new Exception("Error: No output filename provided.");

            if (!String.IsNullOrEmpty(_viewName) && !String.IsNullOrEmpty(_listName))
                return getViewData();
            else if (String.IsNullOrEmpty(_viewName) && !String.IsNullOrEmpty(_listName))
                return getAvailableViews();
            else
                return getAvailableLists();
        }

        private string getAvailableLists()
        {
            using (ClientContext context = new ClientContext(_siteUrl))
            {
                if (!String.IsNullOrEmpty(_username) && !String.IsNullOrEmpty(_password))
                {
                    context.Credentials = new NetworkCredential(_username, _password); // "devcoeetl", "Prod(tn1");
                }
                ListCollection lists = context.Web.Lists;
                context.Load(lists);
                context.ExecuteQuery();

                _columnNames = new List<string>();
                _columnNames.Add("ListTitle");
                _columnNames.Add("ListId");
                _columnNames.Add("DateCreated");
                _columnNames.Add("IsPrivate");
                _columnNames.Add("LastItemDeletedDate");
                _columnNames.Add("LastItemModifiedDate");
                _columnNames.Add("Description");

                _dataRows = new List<Dictionary<string, string>>();
                foreach (List list in lists)
                {
                    Dictionary<string, string> row = new Dictionary<string, string>();
                    row.Add("ListTitle", list.Title);
                    row.Add("ListId", list.Id.ToString());
                    row.Add("DateCreated", list.Created.ToString("s"));
                    row.Add("IsPrivate", list.IsPrivate ? "True" : "False");
                    row.Add("LastItemDeletedDate", list.LastItemDeletedDate.ToString("s"));
                    row.Add("LastItemModifiedDate", list.LastItemModifiedDate.ToString("s"));
                    row.Add("Description", list.Description);
                    _dataRows.Add(row);
                    _rowcount++;
                }

                WriteTextFile();
            }
            return _saveFilename;
        }

        private string getAvailableViews()
        {
            using (ClientContext context = new ClientContext(_siteUrl))
            {
                if (!String.IsNullOrEmpty(_username) && !String.IsNullOrEmpty(_password))
                {
                    context.Credentials = new NetworkCredential("devcoeetl", "Prod(tn1");
                }
                List list = context.Web.Lists.GetByTitle(_listName);
                context.Load(list);
                context.ExecuteQuery();

                ViewCollection views = list.Views;
                context.Load(views);
                context.ExecuteQuery();

                _columnNames = new List<string>();
                _columnNames.Add("ViewTitle");
                _columnNames.Add("ViewId");
                _columnNames.Add("DefaultView");
                _columnNames.Add("PersonalView");
                _columnNames.Add("ReadOnlyView");

                _dataRows = new List<Dictionary<string, string>>();
                foreach (View view in views)
                {
                    Dictionary<string, string> row = new Dictionary<string, string>();
                    row.Add("ViewTitle", view.Title);
                    row.Add("ViewId", view.Id.ToString());
                    row.Add("DefaultView", view.DefaultView ? "True" : "False");
                    row.Add("PersonalView", view.PersonalView ? "True" : "False");
                    row.Add("ReadOnlyView", view.ReadOnlyView ? "True" : "False");
                    _dataRows.Add(row);
                    _rowcount++;
                }

                WriteTextFile();
            }
            return _saveFilename;
        }

        private string getViewData()
        {
            using (ClientContext context = new ClientContext(_siteUrl))
            {
                if (!String.IsNullOrEmpty(_username) && !String.IsNullOrEmpty(_password))
                {
                    context.Credentials = new NetworkCredential(_username, _password);
                }
                else
                {
                    context.Credentials = CredentialCache.DefaultCredentials;
                }
                List list = context.Web.Lists.GetByTitle(_listName);
                context.Load(list);
                context.ExecuteQuery();

                //_viewName = "All Items";
                View view = list.Views.GetByTitle(_viewName);
                context.Load(view);
                context.ExecuteQuery();

                context.Load(view.ViewFields);
                context.ExecuteQuery();
                ViewFieldCollection vfc = view.ViewFields;

                //StringBuilder sbQuery = new StringBuilder("<View><RowLimit>10</RowLimit><ViewFields>");
                StringBuilder sbQuery = new StringBuilder("<View><ViewFields>");

                Console.WriteLine("");
                Console.WriteLine("View requested contains the following columns");
                Console.WriteLine("---------------------------------------------");
                foreach (string field in vfc)
                {
                    sbQuery.AppendFormat("<FieldRef Name=\"{0}\" />", field);
                    Console.WriteLine(field);
                }
                Console.WriteLine();
                sbQuery.Append("</ViewFields></View>");

                //Console.WriteLine("");
                //Console.WriteLine("Query to Run");
                //Console.WriteLine("------------");
                //Console.WriteLine(sbQuery.ToString());
                //Console.WriteLine("");

                CamlQuery query = new CamlQuery();
                //if (!String.IsNullOrEmpty(_camlQuery))
                //    query.ViewXml = _camlQuery;
                //else
                //    query.ViewXml = view.ViewQuery;

                query.ViewXml = sbQuery.ToString();

                ListItemCollection items = list.GetItems(query);
                context.Load(items);
                context.ExecuteQuery();

                _columnNames = new List<string>();
                List<string> keys = new List<string>();
                foreach (ListItem item in items)
                {
                    if (_rowcount == 0)
                    {
                        foreach (KeyValuePair<string, object> kvp in item.FieldValues)
                        {
                            Console.WriteLine(kvp.Key);
                            _columnNames.Add(kvp.Key); //cleanupColumnName(kvp.Key));
                            keys.Add(kvp.Key);
                        }
                    }

                    Dictionary<string, string> row = new Dictionary<string, string>();
                    for (int i = 0; i < keys.Count; i++)
                    {
                        string rawColumnName = keys[i];
                        String value = "";
                        Object objValue = "";
                        if (item.FieldValues.ContainsKey(rawColumnName))
                        {
                            objValue = item.FieldValues[rawColumnName];
                            if (objValue == null)
                                value = "";
                            else if (objValue.GetType().Name.Equals("FieldLookupValue"))
                            {
                                FieldLookupValue fieldLookupValue = (Microsoft.SharePoint.Client.FieldLookupValue)item.FieldValues[rawColumnName];
                                value = fieldLookupValue.LookupValue;

                            }
                            else if (objValue.GetType().Name.Equals("FieldUserValue"))
                            {
                                FieldUserValue fieldUserValue = (Microsoft.SharePoint.Client.FieldUserValue)item.FieldValues[rawColumnName];
                                value = fieldUserValue.LookupValue;
                            }
                            else if (objValue.GetType().Name.Equals("DateTime"))
                            {
                                value = ((DateTime)objValue).ToString("yyyy-MM-ddTHH:mm:ss");
                            }
                            else
                            {
                                value = objValue.ToString();
                            }
                        }
                        row.Add(_columnNames[i], value);
                    }
                    _dataRows.Add(row);
                    _rowcount++;
                }

                WriteTextFile();
            }
            return _saveFilename;
        }

        private void WriteTextFile()
        {
            int rowNumber = 0;
            using (StreamWriter sw = new StreamWriter(_saveFilename))
            {
                int columnNumber = 0;
                if ((rowNumber == 0) && (_columnNames != null))
                {
                    foreach (string columnName in _columnNames)
                    {
                        if (columnNumber > 0) sw.Write(_delimiter);
                        sw.Write(wrap(columnName));
                        columnNumber++;
                    }
                    sw.WriteLine();
                    rowNumber++;
                    columnNumber = 0;
                }

                foreach (Dictionary<string, string> row in _dataRows)
                {
                    columnNumber = 0;
                    foreach (string columnName in _columnNames)
                    {
                        if (columnNumber > 0) sw.Write(_delimiter);
                        if (row.ContainsKey(columnName))
                            sw.Write(wrap(row[columnName]));
                        else
                            sw.Write("");
                        columnNumber++;
                    }
                    sw.WriteLine();
                }
                rowNumber++;
            }
        }

        private string wrap(string value)
        {
            if (String.IsNullOrEmpty(value))
                return value;

            if (value.StartsWith("\"") && value.EndsWith("\""))
                return value;

            bool addQuotes = false;
            if (value.IndexOf("\"") >= 0)
            {
                value = value.Replace("\"", "\"\"");
                addQuotes = true;
            }
            else if (value.IndexOf(_delimiter) >= 0)
            {
                addQuotes = true;
            }

            if (value.IndexOf("\r\n") >= 0)
            {
                value = value.Replace("\r\n", " ");
                addQuotes = true;
            }

            // new line is usually \r\n, but some systems just return \n
            if (value.IndexOf(System.Environment.NewLine) >= 0)
            {
                value = value.Replace(System.Environment.NewLine, " ");
                addQuotes = true;
            }

            if (value.IndexOf("\n") >= 0)
            {
                value = value.Replace("\n", " ");
                addQuotes = true;
            }

            if (addQuotes)
                return "\"" + value + "\"";
            else
                return value;
        }

        private string cleanupColumnName(string name)
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

            if (_columnNames.Contains(newName))
            {
                for (int i = 2; i < 5000; i++)
                {
                    string temp_name = String.Format("{0}_{1}", newName, i);
                    if (!_columnNames.Contains(temp_name))
                    {
                        newName = temp_name;
                        break;
                    }
                }
            }

            Console.WriteLine("{0} => {1}", name, newName);

            return newName;
        }
    }
}
