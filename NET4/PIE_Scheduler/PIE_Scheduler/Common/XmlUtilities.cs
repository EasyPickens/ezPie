﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Xml;
using System.IO;

namespace ScanManager.Common
{
    class XmlUtilities
    {
        public static XmlDocument LoadXmlFile(String sFilename)
        {
            if (!File.Exists(sFilename))
                throw new FileNotFoundException(String.Format("{0} file not found.", sFilename));

            try
            {
                XmlDocument xDoc = new XmlDocument();
                xDoc.Load(sFilename);
                RemoveRemarkedElements(xDoc.DocumentElement);
                return xDoc;
            }
            catch (Exception ex)
            {
                throw new Exception(String.Format("Could not load {0} XML file.", sFilename), ex);
            }
        }

        public static String OptionalAttribute(XmlDocument Settings, String XPath, String Attr)
        { return OptionalAttribute(Settings, XPath, Attr, ""); }

        public static String OptionalAttribute(XmlDocument Settings, String XPath, String Attr, String DefaultValue)
        {
            XmlElement ele = (XmlElement)Settings.SelectSingleNode(XPath);
            if ((ele == null) || !ele.HasAttribute(Attr))
                return DefaultValue;

            return ele.GetAttribute(Attr);
        }

        public static String RequiredAttribute(XmlDocument Settings, String XPath, String Attr)
        { return RequiredAttribute(Settings, XPath, Attr, String.Format("Missing required {0} attribute for {1} node.", Attr, XPath)); }

        public static String RequiredAttribute(XmlDocument Settings, String XPath, String Attr, String ErrorMessage)
        {
            XmlElement ele = (XmlElement)Settings.SelectSingleNode(XPath);
            if ((ele == null) || !ele.HasAttribute(Attr))
                throw new Exception(String.Format(ErrorMessage, Attr));

            return ele.GetAttribute(Attr);
        }

        private static void RemoveRemarkedElements(XmlNode nodeDefinition)
        {
            XmlNodeList nlRemarked = nodeDefinition.SelectNodes(".//Remark");
            if (nlRemarked.Count == 0) return;

            foreach (XmlNode nodeRemarked in nlRemarked)
                nodeDefinition.RemoveChild(nodeRemarked);
        }
    }
}
