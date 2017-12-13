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

package com.fanniemae.ezpie.actions;

import java.util.HashMap;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.w3c.dom.Element;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.common.FileUtilities;
import com.fanniemae.ezpie.common.PieException;

/**
 * 
 * @author Tara Tritt
 * @author Rick Monson
 * @since 2017-03-16
 * 
 */

public class SendEmail extends Action {

	protected String _username;
	protected String _password;	
	protected String _smtpHost;
	protected int _smtpPort;
	protected String _senderEmail;
	protected String _recieverEmail;
	protected String _mailSubject;
	protected String _mailBody;
	
	protected String _connID;
	protected Element _conn;

	public SendEmail(SessionManager session, Element action) {
		super(session, action, false);
		 _connID = requiredAttribute("ConnectionName", null);
		 _conn = _session.getConnection(_connID);
		 
		_username = requiredAttribute(_conn,"Username");
		_password = requiredAttribute(_conn,"Password");
		_smtpHost = requiredAttribute(_conn,"Host");
		_smtpPort = Integer.parseInt(requiredAttribute(_conn,"Port"));
		_senderEmail = requiredAttribute(_conn,"SenderEmail");
		
		_recieverEmail = requiredAttribute("RecieverEmail");		
		_mailSubject = requiredAttribute("MailSubject");
		_mailBody = optionalAttribute("MailBody", null);
		if(_mailBody == null){
			String mailBodyFilename = requiredAttribute("MailBodyFilename", String.format("Missing a value for MailBody or MailBodyFilename on the %s element.", action.getNodeName()));
			_mailBody = FileUtilities.loadFile(mailBodyFilename);
			_mailBody = session.resolveTokens(_mailBody);
			_session.addLogMessageHtml("", "MailBody", _mailBody);
		}	
	}

	@Override
	public String executeAction(HashMap<String, String> dataTokens) {
		_session.setDataTokens(dataTokens);

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", _smtpHost);
        props.put("mail.smtp.port", _smtpPort);
        
        Session session = Session.getInstance(props,
          new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(_username, _password);
            }
          });

        try {

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(_senderEmail));
            message.setRecipients(Message.RecipientType.TO,
                InternetAddress.parse(_recieverEmail));
            message.setSubject(_mailSubject);
            message.setContent(_mailBody, "text/html; charset=utf-8");

            Transport.send(message);

            _session.addLogMessage("", "SendEmail", String.format("Sent email to %s", _recieverEmail));

        } catch (MessagingException e) {
            throw new PieException(String.format("Error while trying to send email. Message is %s", e.getMessage()), e);
        }
        _session.clearDataTokens();
		return null;
	}

}
