using System;
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

        private static void RemoveRemarkedElements(XmlNode nodeDefinition)
        {
            XmlNodeList nlRemarked = nodeDefinition.SelectNodes(".//Remark");
            if (nlRemarked.Count == 0) return;

            foreach (XmlNode nodeRemarked in nlRemarked)
                nodeDefinition.RemoveChild(nodeRemarked);
        }
    }
}
