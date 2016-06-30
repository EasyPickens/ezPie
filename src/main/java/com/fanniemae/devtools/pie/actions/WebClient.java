package com.fanniemae.devtools.pie.actions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.fanniemae.devtools.pie.SessionManager;
import com.fanniemae.devtools.pie.common.StringUtilities;
import com.fanniemae.devtools.pie.common.XmlUtilities;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.DefaultCredentialsProvider;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlFileInput;
import com.gargoylesoftware.htmlunit.html.HtmlOption;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlRadioButtonInput;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;

public class WebClient extends Action {

	protected int _javascriptWait = 30;

	protected String _connID;

	protected String _proxyHost = null;
	protected int _proxyPort = 80;

	protected Element _conn;

	protected HtmlPage _htmlpage;
	protected HtmlElement _htmlelement;

	protected com.gargoylesoftware.htmlunit.WebClient _webClient;

	public WebClient(SessionManager session, Element action) {
		super(session, action, false);

		_javascriptWait = StringUtilities.toInteger(_session.getAttribute(action, "JavascriptWait"), 30);
		_connID = optionalAttribute("ConnectionID", null);
		_conn = _session.getConnection(_connID);
	}

	@Override
	public String execute() {
		// Get a list of web client steps
		try (final com.gargoylesoftware.htmlunit.WebClient webClient = connect()) {
			_webClient = webClient;
			webActions(_action);
		} catch (Exception ex) {
			throw new RuntimeException(String.format("WebClient error %s", ex.getMessage()), ex);
		}
		return null;
	}

	protected com.gargoylesoftware.htmlunit.WebClient connect() {
		if (_conn == null) {
			_session.addLogMessage("", "", "Setting up web client without proxy");
			return new com.gargoylesoftware.htmlunit.WebClient(BrowserVersion.FIREFOX_38);
		} else {
			String proxyUsername = _session.getAttribute(_conn, "ProxyUsername");
			String proxyPassword = _session.getAttribute(_conn, "ProxyPassword");
			String proxyHost = _session.getAttribute(_conn, "ProxyHost");
			int proxyPort = StringUtilities.toInteger(_session.getAttribute(_conn, "ProxyPort"), 80);
			_session.addLogMessage("", "Connect", String.format("Using proxy %s:%d", proxyHost, proxyPort));

			com.gargoylesoftware.htmlunit.WebClient webClient = new com.gargoylesoftware.htmlunit.WebClient(BrowserVersion.FIREFOX_38, proxyHost, proxyPort);
			// set proxy username and password
			final DefaultCredentialsProvider credentialsProvider = (DefaultCredentialsProvider) webClient.getCredentialsProvider();
			credentialsProvider.addCredentials(proxyUsername, proxyPassword);
			return webClient;
		}
	}

	protected void webActions(Node node) {
		NodeList nl = XmlUtilities.selectNodes(node, "*");
		int steps = nl.getLength();
		_session.addLogMessage("", "WebClient", String.format("%,d steps found", steps));
		for (int i = 0; i < steps; i++) {
			Node nodeStep = nl.item(i);
			String stepAction = nodeStep.getNodeName();
			switch (stepAction) {
			case "Navigate":
				navigate(nodeStep);
				break;
			case "SelectElement":
				selectElement(nodeStep);
				break;
			case "InputTextFromSelected":
				inputTextFromSelected(nodeStep);
				break;
			case "InputText":
				inputText(nodeStep);
				break;
			case "ClickElementFromSelected":
				clickElementFromSelected(nodeStep);
				break;
			case "ClickButton":
			case "ClickElement":
				clickElement(nodeStep);
				break;
			case "CheckRadioFromSelected":
				checkRadioFromSelected(nodeStep);
				break;
			case "SelectOption":
				selectOption(nodeStep);
				break;
			case "ReadAttribute":
				readAttribute(nodeStep);
				break;
			case "DownloadFile":
				downloadFile(nodeStep);
				break;
			case "UploadFile":
				uploadFile(nodeStep);
				break;
			case "FoundElementText":
				if (foundElementText(nodeStep))
					webActions(nodeStep);
				break;
			case "DidNotFindElementText":
				if (!foundElementText(nodeStep))
					webActions(nodeStep);
				break;
			}
		}
	}

	private boolean foundElementText(Node nodeStep) {
		optionalAttribute(nodeStep, "Comment", null);
		String xpath = requiredAttribute(nodeStep, "XPath");
		validateWebState(xpath, nodeStep.getNodeName(), false);

		String value = requiredAttribute(nodeStep, "Value");

		_session.addLogMessage("", "FoundElementText", String.format("Locating HTML form at %s", xpath));

		String function = xpath + "[text()[normalize-space(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')) = '" + value.toLowerCase() + "']]";

		HtmlElement element = _htmlpage.getFirstByXPath(function);

		if (element == null) {
			_session.addLogMessage("", "", String.format("FoundElementText Did not find matching element at %s", function));
			return false;
		} else {
			_session.addLogMessage("", "", String.format("FoundElementText Did find matching element at %s", function));
			return true;
		}
	}

	protected void navigate(Node nodeStep) {
		int jswait = StringUtilities.toInteger(_session.getAttribute(nodeStep, "JavascriptWait"), _javascriptWait);
		optionalAttribute(nodeStep, "Comment", null);
		String url = requiredAttribute(nodeStep, "URL");

		try {
			_session.addLogMessage("", "Navigation", String.format("Requesting page from %s (wait up to %,d seconds)", url, jswait));

			_htmlpage = _webClient.getPage(url);
			_webClient.waitForBackgroundJavaScript(jswait * 1000);
		} catch (Exception ex) {
			throw new RuntimeException(String.format("WebClient navigation error for %s. %s", url, ex.getMessage()), ex);
		}
	}

	protected void selectElement(Node nodeStep) {
		optionalAttribute(nodeStep, "Comment", null);
		String xpath = requiredAttribute(nodeStep, "XPath");
		validateWebState(xpath, nodeStep.getNodeName(), false);

		String value = optionalAttribute(nodeStep, "Value", "");

		String childXpath = optionalAttribute(nodeStep, "ChildXPath", "");

		while (childXpath.startsWith("/")) {
			childXpath = childXpath.substring(1);
		}

		String function = value.equals("") ? xpath : xpath + "[" + (!childXpath.equals("") ? childXpath + "//" : "") + "text()[normalize-space(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')) = '" + value.toLowerCase() + "']]";
		_session.addLogMessage("", "SelectElement", String.format("Locating HTML element at %s", function));

		_htmlelement = _htmlpage.getFirstByXPath(function);
		if (_htmlelement == null) {
			throw new RuntimeException(String.format("WebClient SelectElement did not find an HTML element at %s", function));
		}
	}

	protected void inputText(Node nodeStep) {
		int jswait = StringUtilities.toInteger(_session.getAttribute(nodeStep, "JavascriptWait"), _javascriptWait);
		optionalAttribute(nodeStep, "Comment", null);
		String xpath = requiredAttribute(nodeStep, "XPath");
		validateWebState(xpath, nodeStep.getNodeName(), false);

		String value = requiredAttribute(nodeStep, "Value");

		_session.addLogMessage("", "InputText", String.format("Locating HTML input field at %s and inputing text.", xpath));

		HtmlInput field = _htmlpage.getFirstByXPath(xpath);
		if (field == null) {
			throw new RuntimeException(String.format("WebClient InputText did not find a matching form field for the XPath %s", xpath));
		}
		field.setValueAttribute(value);
		_webClient.waitForBackgroundJavaScript(jswait * 1000);
	}

	protected void inputTextFromSelected(Node nodeStep) {
		int jswait = StringUtilities.toInteger(_session.getAttribute(nodeStep, "JavascriptWait"), _javascriptWait);
		optionalAttribute(nodeStep, "Comment", null);
		String xpath = optionalAttribute(nodeStep, "XPath", "//");
		validateWebState(xpath, nodeStep.getNodeName());

		String value = requiredAttribute(nodeStep, "Value");

		HtmlInput field;
		if (StringUtilities.isNotNullOrEmpty(xpath) && !xpath.equals("//")) {
			field = _htmlelement.getFirstByXPath(xpath);
		} else {
			field = (HtmlInput) _htmlelement;
		}

		if (field == null) {
			throw new RuntimeException(String.format("WebClient InputTextFromSelected did not find a matching input field for the XPath %s", xpath));
		}

		_session.addLogMessage("", "InputTextFromSelected", String.format("Locating HTML input field from selected element at %s and inputing text.", xpath));

		field.setValueAttribute(value);
		_webClient.waitForBackgroundJavaScript(jswait * 1000);
	}

	protected void clickElementFromSelected(Node nodeStep) {
		int jswait = StringUtilities.toInteger(_session.getAttribute(nodeStep, "JavascriptWait"), _javascriptWait);
		optionalAttribute(nodeStep, "Comment", null);
		String xpath = optionalAttribute(nodeStep, "XPath", "//");
		validateWebState(xpath, nodeStep.getNodeName());
		
		HtmlElement element;
		if (StringUtilities.isNotNullOrEmpty(xpath) && !xpath.equals("//")) {
			element = _htmlelement.getFirstByXPath(xpath);
		} else {
			element = _htmlelement;
		}

		if (element == null) {
			throw new RuntimeException(String.format("WebClient ClickElementFromSelected did not find a matching element for the XPath %s (wait up to %,d seconds)", xpath, jswait));
		}

		_session.addLogMessage("", "ClickElementFromSelected", String.format("Locating HTML element from selected element at %s and clicking.", xpath));

		try {
			_htmlpage = element.click();
			_webClient.waitForBackgroundJavaScript(jswait * 1000);
		} catch (IOException e) {
			throw new RuntimeException(String.format("WebClient ClickElementFromSelected error while trying to submit form. %s", e.getMessage()), e);
		}
	}

	protected void clickElement(Node nodeStep) {
		int jswait = StringUtilities.toInteger(_session.getAttribute(nodeStep, "JavascriptWait"), _javascriptWait);
		optionalAttribute(nodeStep, "Comment", null);
		String xpath = requiredAttribute(nodeStep, "XPath");
		validateWebState(xpath, nodeStep.getNodeName(), false);

		_session.addLogMessage("", "ClickElement", String.format("Locating HTML element at %s and clicking.", xpath));

		HtmlElement element = _htmlpage.getFirstByXPath(xpath);
		if (element == null) {
			throw new RuntimeException(String.format("WebClient ClickElement did not find a matching element for the XPath %s (wait up to %,d seconds)", xpath, jswait));
		}
		try {
			_htmlpage = element.click();
			_webClient.waitForBackgroundJavaScript(jswait * 1000);
		} catch (IOException e) {
			throw new RuntimeException(String.format("WebClient ClickElement error while trying to click element. %s", e.getMessage()), e);
		}
	}

	protected void checkRadioFromSelected(Node nodeStep) {
		int jswait = StringUtilities.toInteger(_session.getAttribute(nodeStep, "JavascriptWait"), _javascriptWait);
		optionalAttribute(nodeStep, "Comment", null);
		String xpath = optionalAttribute(nodeStep, "XPath", "//");
		validateWebState(xpath, nodeStep.getNodeName());
		
		HtmlRadioButtonInput radio;
		if (StringUtilities.isNotNullOrEmpty(xpath) && !xpath.equals("//")) {
			radio = _htmlelement.getFirstByXPath(xpath);
		} else {
			radio = (HtmlRadioButtonInput) _htmlelement;
		}

		if (radio == null) {
			throw new RuntimeException(String.format("WebClient CheckRadioFromSelected did not find a matching element for the XPath %s (wait up to %,d seconds)", xpath, jswait));
		}

		_session.addLogMessage("", "CheckRadioFromSelected", String.format("Locating HTML radio from selected element at %s and clicking.", xpath));

		_htmlpage = (HtmlPage) radio.setChecked(true);
		_webClient.waitForBackgroundJavaScript(jswait * 1000);
	}

	protected void selectOption(Node nodeStep) {
		int jswait = StringUtilities.toInteger(_session.getAttribute(nodeStep, "JavascriptWait"), _javascriptWait);
		optionalAttribute(nodeStep, "Comment", null);
		String xpath = requiredAttribute(nodeStep, "XPath");
		validateWebState(xpath, nodeStep.getNodeName(), false);

		String value = requiredAttribute(nodeStep, "Value");

		_session.addLogMessage("", "SelectOption", String.format("Locating Select and selecting option.", xpath));

		HtmlSelect select = (HtmlSelect) _htmlpage.getFirstByXPath(xpath);
		if (select == null) {
			throw new RuntimeException(String.format("WebClient SelectOption did not find a matching select for the XPath %s (wait up to %,d seconds)", xpath, jswait));
		}

		HtmlOption option = _htmlpage.getFirstByXPath(xpath + "/option[translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz') = '" + value.toLowerCase() + "']");
		if (option == null) {
			throw new RuntimeException(String.format("WebClient SelectOption did not find a matching option at %s (wait up to %,d seconds)", xpath + "/option[translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz') = '" + value.toLowerCase() + "']", jswait));
		}

		_htmlpage = select.setSelectedAttribute(option, true);
		_webClient.waitForBackgroundJavaScript(jswait * 1000);
	}

	protected void readAttribute(Node nodeStep) {
		optionalAttribute(nodeStep, "Comment", null);
		String xpath = requiredAttribute(nodeStep, "XPath");
		validateWebState(xpath, nodeStep.getNodeName(), false);

		String id = requiredAttribute(nodeStep, "ID");

		String attrName = requiredAttribute(nodeStep, "AttributeName");

		_session.addLogMessage("", "ReadAttribute", String.format("Locating element at %s and reading %s attribute value.", xpath, attrName));

		DomElement element = _htmlpage.getFirstByXPath(xpath);
		if (element == null) {
			throw new RuntimeException(String.format("WebClient ReadAttribute did not find a matching element for the XPath %s", xpath));
		}
		String value = element.getAttribute(attrName);
		_session.addToken("WebClient", id, value);
	}

	protected void downloadFile(Node nodeStep) {
		int jswait = StringUtilities.toInteger(_session.getAttribute(nodeStep, "JavascriptWait"), _javascriptWait);
		optionalAttribute(nodeStep, "Comment", null);

		String url = requiredAttribute(nodeStep, "URL");
		
		String filename = requiredAttribute(nodeStep, "SaveAs");

		_session.addLogMessage("", "DownloadFile", String.format("Downloading and saving file to %s. (wait up to %,d seconds)", filename, jswait));

		try {
			Page page = _webClient.getPage(url);
			_webClient.waitForBackgroundJavaScript(jswait * 1000);

			InputStream is = page.getWebResponse().getContentAsStream();
			try (OutputStream os = new FileOutputStream(filename, false)) {
				byte[] buffer = new byte[4096];
				int bytesRead;
				// read from is to buffer
				while ((bytesRead = is.read(buffer)) != -1) {
					os.write(buffer, 0, bytesRead);
				}
				os.flush();
				os.close();
			} catch (IOException e) {
				throw new RuntimeException(String.format("WebClient DownloadFile error for %s. %s", filename, e.getMessage()), e);
			} finally {
				if (is != null)
					is.close();
			}
		} catch (Exception ex) {
			throw new RuntimeException(String.format("WebClient navigation error for %s. %s", url, ex.getMessage()), ex);
		}
	}

	protected void uploadFile(Node nodeStep) {
		int jswait = StringUtilities.toInteger(_session.getAttribute(nodeStep, "JavascriptWait"), _javascriptWait);
		optionalAttribute(nodeStep, "Comment", null);
		String xpath = requiredAttribute(nodeStep, "XPath");
		validateWebState(xpath, nodeStep.getNodeName(), false);

		String filePath = requiredAttribute(nodeStep, "FilePath");

		_session.addLogMessage("", "UploadFile", String.format("Locating HTML input and uploading files.", xpath));

		File file = new File(filePath);

		if (!file.exists()) {
			throw new RuntimeException(String.format("No file/folder exists at the path %s.", filePath));
		}

		HtmlFileInput input = _htmlpage.getFirstByXPath(xpath);
		if (input == null) {
			throw new RuntimeException(String.format("WebClient UploadFile did not find a matching element for the XPath %s", xpath));
		}

		if (file.isDirectory()) {
			File[] directoryListing = file.listFiles();
			for (File child : directoryListing) {
				if (child.isFile()) {
					_session.addLogMessage("", "", "UploadFile uploading file from " + filePath + "/" + child.getName());
					input.setValueAttribute(child.getPath());
					_webClient.waitForBackgroundJavaScript(jswait * 1000);
				}
			}
		} else if (file.isFile()) {
			_session.addLogMessage("", "", "UploadFile uploading file from " + filePath);
			input.setValueAttribute(filePath);
			_webClient.waitForBackgroundJavaScript(jswait * 1000);
		}

	}

	protected void validateWebState(String xpath, String webStep) {
		validateWebState(xpath, webStep, true);
	}

	protected void validateWebState(String xpath, String webStep, boolean requiresElement) {
		if (StringUtilities.isNullOrEmpty(xpath)) {
			throw new RuntimeException(String.format("%s element missing the required XPath value.", webStep));
		} else if (_htmlpage == null) {
			throw new RuntimeException(String.format("No HTML page defined.  A Navigate element must precede the %s element.", webStep));
		} else if (requiresElement && (_htmlelement == null)) {
			throw new RuntimeException(String.format("No HTML ELEMENT defined.  A Navigate element and SelectElement must precede the %s element.", webStep));
		}
	}
}
