package com.fanniemae.devtools.pie.actions;

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
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class WebClient extends Action {

	protected int _javascriptWait = 30;

	protected String _connID;

	protected String _proxyHost = null;
	protected int _proxyPort = 80;

	protected Element _conn;

	protected HtmlPage _htmlpage;
	protected HtmlForm _htmlform;

	protected com.gargoylesoftware.htmlunit.WebClient _webClient;

	public WebClient(SessionManager session, Element action) {
		super(session, action, false);

		_javascriptWait = StringUtilities.toInteger(_session.getAttribute(action, "JavascriptWait"), 30);
		_connID = _session.getAttribute(action, "ConnectionID");
		_session.addLogMessage("", "ConnectionID", _connID);
		_conn = _session.getConnection(_connID);
		if (_conn == null) {
			throw new RuntimeException(String.format("%s connection element not found in the settings file.", _connID));
		}
	}

	@Override
	public String execute() {
		// Get a list of web client steps
		NodeList nl = XmlUtilities.selectNodes(_action, "*");
		int steps = nl.getLength();
		_session.addLogMessage("", "WebClient", String.format("%,d steps found", steps));
		try (final com.gargoylesoftware.htmlunit.WebClient webClient = connect()) {
			_webClient = webClient;
			for (int i = 0; i < steps; i++) {
				Node nodeStep = nl.item(i);
				String stepAction = nodeStep.getNodeName();
				switch (stepAction) {
				case "Navigate":
					navigate(nodeStep);
					break;
				case "SelectForm":
					selectForm(nodeStep);
					break;
				case "InputText":
					inputText(nodeStep);
					break;
				case "ClickButton":
					clickButton(nodeStep);
					break;
				case "ReadAttribute":
					readAttribute(nodeStep);
					break;
				case "DownloadFile":
					downloadFile(nodeStep);
					break;
				}
			}
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

	protected void navigate(Node nodeStep) {
		int jswait = StringUtilities.toInteger(_session.getAttribute(nodeStep, "JavascriptWait"), _javascriptWait);
		String comment = _session.getAttribute(nodeStep, "Comment");
		String url = _session.getAttribute(nodeStep, "URL");
		if (StringUtilities.isNullOrEmpty(url)) {
			throw new RuntimeException("Navigation step element is missing the required destination URL.");
		}
		try {
			_session.addLogMessage("", "Navigation", String.format("Requesting page from %s (wait up to %,d seconds)", url, jswait));
			if (StringUtilities.isNotNullOrEmpty(comment)) {
				_session.addLogMessage("", "", comment);
			}

			_htmlpage = _webClient.getPage(url);
			_webClient.waitForBackgroundJavaScript(jswait * 1000);
		} catch (Exception ex) {
			throw new RuntimeException(String.format("WebClient navigation error for %s. %s", url, ex.getMessage()), ex);
		}
	}

	protected void selectForm(Node nodeStep) {
		String comment = _session.getAttribute(nodeStep, "Comment");
		String xpath = _session.getAttribute(nodeStep, "XPath");
		validateWebState(xpath, nodeStep.getNodeName(), false);

		_session.addLogMessage("", "SelectForm", String.format("Locating HTML form at %s", xpath));
		if (StringUtilities.isNotNullOrEmpty(comment)) {
			_session.addLogMessage("", "", comment);
		}

		_htmlform = _htmlpage.getFirstByXPath(xpath);
		if (_htmlform == null) {
			throw new RuntimeException(String.format("WebClient SelectForm did not find an HTML form for the XPath %s", xpath));
		}
	}

	protected void inputText(Node nodeStep) {
		String comment = _session.getAttribute(nodeStep, "Comment");
		String xpath = _session.getAttribute(nodeStep, "XPath");
		validateWebState(xpath, nodeStep.getNodeName());

		String value = _session.getAttribute(nodeStep, "Value");
		if (StringUtilities.isNullOrEmpty(value)) {
			value = "";
		}

		_session.addLogMessage("", "InputText", String.format("Locating HTML form field at %s and inputing text.", xpath));
		if (StringUtilities.isNotNullOrEmpty(comment)) {
			_session.addLogMessage("", "", comment);
		}

		HtmlInput field = _htmlform.getFirstByXPath(xpath);
		if (field == null) {
			throw new RuntimeException(String.format("WebClient InputText did not find a matching form field for the XPath %s", xpath));
		}		
		field.setValueAttribute(value);
	}

	protected void clickButton(Node nodeStep) {
		int jswait = StringUtilities.toInteger(_session.getAttribute(nodeStep, "JavascriptWait"), _javascriptWait);
		String comment = _session.getAttribute(nodeStep, "Comment");
		String xpath = _session.getAttribute(nodeStep, "XPath");
		validateWebState(xpath, nodeStep.getNodeName());

		_session.addLogMessage("", "ClickButton", String.format("Locating HTML form button at %s and clicking.", xpath));
		if (StringUtilities.isNotNullOrEmpty(comment)) {
			_session.addLogMessage("", "", comment);
		}

		HtmlButton button = _htmlform.getFirstByXPath(xpath);
		if (button == null) {
			throw new RuntimeException(String.format("WebClient ClickButton did not find a matching submit button for the XPath %s (wait up to %,d seconds)", xpath, jswait));
		}
		try {
			_htmlpage = button.click();
			_webClient.waitForBackgroundJavaScript(jswait * 1000);
		} catch (IOException e) {
			throw new RuntimeException(String.format("WebClient ClickButton error while trying to submit form. %s", e.getMessage()), e);
		}
	}

	protected void readAttribute(Node nodeStep) {
		String comment = _session.getAttribute(nodeStep, "Comment");
		String xpath = _session.getAttribute(nodeStep, "XPath");
		validateWebState(xpath, nodeStep.getNodeName(), false);

		String id = _session.getAttribute(nodeStep, "ID");
		if (StringUtilities.isNullOrEmpty(id)) {
			throw new RuntimeException("WebClient ReadNode is missing the required an ID value.  The ID is used to populate the token @WebClient.<ID>~");
		}

		String attrName = _session.getAttribute(nodeStep, "AttributeName");
		if (StringUtilities.isNullOrEmpty(id)) {
			throw new RuntimeException("WebClient ReadNode is missing the required an AttributeName value.");
		}

		_session.addLogMessage("", "ReadAttribute", String.format("Locating element at %s and reading %s attribute value.", xpath, attrName));
		if (StringUtilities.isNotNullOrEmpty(comment)) {
			_session.addLogMessage("", "", comment);
		}
		DomElement element = _htmlpage.getFirstByXPath(xpath);
		if (element == null) {
			throw new RuntimeException(String.format("WebClient ReadAttrbute did not find a matching element for the XPath %s", xpath));
		}
		String value = element.getAttribute(attrName);
		_session.addToken("WebClient", id, value);
	}

	protected void downloadFile(Node nodeStep) {
		int jswait = StringUtilities.toInteger(_session.getAttribute(nodeStep, "JavascriptWait"), _javascriptWait);
		String comment = _session.getAttribute(nodeStep, "Comment");

		String url = _session.getAttribute(nodeStep, "URL");
		if (StringUtilities.isNullOrEmpty(url)) {
			throw new RuntimeException("WebClient DownloadFile element is missing the required URL.");
		}

		String filename = _session.getAttribute(nodeStep, "SaveAs");
		if (StringUtilities.isNullOrEmpty(filename)) {
			throw new RuntimeException("WebClient DownloadFile element SaveAs value is missing.");
		}

		_session.addLogMessage("", "DownloadFile", String.format("Downloading and saving file to %s. (wait up to %,d seconds)", filename, jswait));
		if (StringUtilities.isNotNullOrEmpty(comment)) {
			_session.addLogMessage("", "", comment);
		}
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

	protected void validateWebState(String xpath, String webStep) {
		validateWebState(xpath, webStep, true);
	}

	protected void validateWebState(String xpath, String webStep, boolean requiresForm) {
		if (StringUtilities.isNullOrEmpty(xpath)) {
			throw new RuntimeException(String.format("%s element missing the required XPath value.", webStep));
		} else if (_htmlpage == null) {
			throw new RuntimeException(String.format("No HTML page defined.  A Navigate element must precede the %s element.", webStep));
		} else if (requiresForm && (_htmlform == null)) {
			throw new RuntimeException(String.format("No HTML FORM defined.  A Navigate element and SelectForm element must precede the %s element.", webStep));
		}
	}

}
