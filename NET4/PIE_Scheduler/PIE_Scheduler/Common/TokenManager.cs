using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Xml;

namespace ScanManager.Common
{
    class TokenManager
    {
        protected Dictionary<String, Dictionary<String, String>> _tokens = new Dictionary<string, Dictionary<string, string>>();

        public TokenManager(XmlDocument Settings)
        {
            XmlNodeList nlTokenBlocks = Settings.DocumentElement.SelectNodes("//Tokens");
            foreach (XmlNode node in nlTokenBlocks)
            {
                XmlNodeList nlTokens = node.SelectNodes("*");
                foreach (XmlNode nodeToken in nlTokens)
                {

                    XmlElement token = (XmlElement)nodeToken;
                    String tokenType = token.Name;

                    Dictionary<String, String> tokens = null;
                    if (_tokens.ContainsKey(tokenType)) { tokens = _tokens[tokenType]; }
                    else { tokens = new Dictionary<string, string>(); _tokens.Add(tokenType, tokens); }

                    foreach (XmlAttribute xA in token.Attributes)
                    {
                        String tokenKey = xA.Name;
                        String tokenValue = xA.Value;
                        if (tokens.ContainsKey(tokenKey)) { tokens[xA.Name] = tokenValue; }
                        else { tokens.Add(tokenKey, tokenValue); }
                    }
                    _tokens[tokenType] = tokens;
                }
            }
        }

        public String Resolve(String tokenType, String tokenKey)
        { return Resolve(tokenType, tokenKey, "No value defined for the token @{0}.{1}~ "); }

        public String Resolve(String tokenType, String tokenKey, String errorMessage)
        {
            if (!_tokens.ContainsKey(tokenType) || !_tokens[tokenType].ContainsKey(tokenKey))
                throw new Exception(String.Format(errorMessage, tokenType, tokenKey));

            return _tokens[tokenType][tokenKey];
        }

        public String ResolveOptional(String tokenType, String tokenKey)
        { return ResolveOptional(tokenType, tokenKey, ""); }

        public String ResolveOptional(String tokenType, String tokenKey, String defaultValue)
        {
            if (!_tokens.ContainsKey(tokenType) || !_tokens[tokenType].ContainsKey(tokenKey))
                return defaultValue;

            return _tokens[tokenType][tokenKey];
        }
    }
}
