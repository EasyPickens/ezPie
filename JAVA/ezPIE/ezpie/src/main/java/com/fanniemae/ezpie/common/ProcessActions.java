/**
 *  
 * Copyright (c) 2017 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPie/wiki/License
 * 
 * ezPIE® is a registered trademark of Fannie Mae
 * 
 */

package com.fanniemae.ezpie.common;

import java.util.HashMap;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fanniemae.ezpie.SessionManager;
import com.fanniemae.ezpie.actions.Action;
import com.fanniemae.ezpie.actions.CastCreateSchemas;
import com.fanniemae.ezpie.actions.CastScan;
import com.fanniemae.ezpie.actions.ComponentScan;
import com.fanniemae.ezpie.actions.Compression;
import com.fanniemae.ezpie.actions.Copy;
import com.fanniemae.ezpie.actions.CreateJavaProjectFiles;
import com.fanniemae.ezpie.actions.DataLoop;
import com.fanniemae.ezpie.actions.DataSet;
import com.fanniemae.ezpie.actions.Delete;
import com.fanniemae.ezpie.actions.DeleteEmpty;
import com.fanniemae.ezpie.actions.Directory;
import com.fanniemae.ezpie.actions.ExecuteSql;
import com.fanniemae.ezpie.actions.ExportDelimited;
import com.fanniemae.ezpie.actions.GetSourceCode;
import com.fanniemae.ezpie.actions.GitClone;
import com.fanniemae.ezpie.actions.HighlightScan;
import com.fanniemae.ezpie.actions.IfElement;
import com.fanniemae.ezpie.actions.LogComment;
import com.fanniemae.ezpie.actions.MakeDirectory;
import com.fanniemae.ezpie.actions.Maven;
import com.fanniemae.ezpie.actions.Move;
import com.fanniemae.ezpie.actions.Rename;
import com.fanniemae.ezpie.actions.RunCommand;
import com.fanniemae.ezpie.actions.Schedule;
import com.fanniemae.ezpie.actions.SendEmail;
import com.fanniemae.ezpie.actions.Sleep;
import com.fanniemae.ezpie.actions.Svn;
import com.fanniemae.ezpie.actions.TensorFlow;
import com.fanniemae.ezpie.actions.Tokens;
import com.fanniemae.ezpie.actions.UpdateStatus;
import com.fanniemae.ezpie.actions.VerifyJavaFiles;
import com.fanniemae.ezpie.actions.VersionFile;
import com.fanniemae.ezpie.actions.WebClient;
import com.fanniemae.ezpie.actions.XmlEdit;

/**
 * 
 * @author Rick Monson (https://www.linkedin.com/in/rick-monson/)
 * @since 2015-12-22
 * 
 */

public class ProcessActions {
	
	public static String run(SessionManager session, NodeList actionList, HashMap<String, String> dataTokens) {
		int length = actionList.getLength();
		if (length == 0)
			return "";

		boolean stopProcessing = false;
		Action action = null;
		try {
			for (int i = 0; i < length; i++) {
				Element actionElement = (Element) actionList.item(i);
				switch (actionElement.getNodeName()) {
				case "IncludeSharedElement":
					throw new PieException("Please rename IncludeSharedElement to ImportSharedElement");
				case "SharedElement":
				case "Note":
					continue;
				case "Tokens":
				case "StaticTokens":
					action = new Tokens(session, actionElement);
					break;
				case "RunCommand":
					// Run an external command or batch file
					action = new RunCommand(session, actionElement);
					break;
				case "DataSet":
					// Pull data and process
					action = new DataSet(session, actionElement);
					break;
				case "Log":
				case "LogComment":
					action = new LogComment(session, actionElement);
					break;
				case "Export":
					action = new ExportDelimited(session, actionElement);
					break;
				case "SvnCheckout":
				case "Svn":
					action = new Svn(session, actionElement);
					break;
				case "Directory":
					action = new Directory(session, actionElement);
					break;
				case "WebClient":
					action = new WebClient(session, actionElement);
					break;
				case "Zip":
				case "UnZip":
					action = new Compression(session, actionElement);
					break;
				case "ComponentScan":
					action = new ComponentScan(session, actionElement);
					break;
				case "Maven":
					action = new Maven(session, actionElement);
					break;
				case "Copy":
					action = new Copy(session, actionElement);
					break;
				case "Move":
					action = new Move(session, actionElement);
					break;
				case "Delete":
					action = new Delete(session, actionElement);
					break;
				case "DeleteEmpty":
					action = new DeleteEmpty(session, actionElement);
					break;
				case "Rename":
					action = new Rename(session, actionElement);
					break;
				case "HighlightScan":
					action = new HighlightScan(session, actionElement);
					break;
				case "MakeDirectory":
					action = new MakeDirectory(session, actionElement);
					break;
				case "VersionFile":
					action = new VersionFile(session, actionElement);
					break;
				case "CastScan":
					action = new CastScan(session, actionElement);
					break;
				case "CastCreateSchemas":
					action = new CastCreateSchemas(session, actionElement);
					break;
				case "ExecuteSql":
					action = new ExecuteSql(session, actionElement);
					break;
				case "Sleep":
					action = new Sleep(session, actionElement);
					break;
				case "Stop":
					if (!"True".equalsIgnoreCase(session.getAttribute(actionElement, "Silent")))
						session.addLogMessage("Stop", "Control Action", "Stopping definition processing. No error.");
					action = null;
					stopProcessing = true;
					break;
				case "UpdateStatus":
					action = new UpdateStatus(session, actionElement);
					break;
				case "XmlEdit":
					action = new XmlEdit(session, actionElement);
					break;
				case "If":
					action = new IfElement(session, actionElement);
					IfElement condition = (IfElement) action;
					if (condition.evalToBoolean(dataTokens)) {
						NodeList childActions = XmlUtilities.selectNodes(actionElement, "*");
						run(session, childActions, dataTokens);
					}
					break;
				case "VerifyJavaFiles":
					action = new VerifyJavaFiles(session, actionElement);
					break;
				case "CreateJavaProjectFiles":
					action = new CreateJavaProjectFiles(session, actionElement);
					break;
				case "SendEmail":
					action = new SendEmail(session, actionElement);
					break;
				case "GetSourceCode":
					action = new GetSourceCode(session, actionElement);
					break;
				case "Loop":
					action = new DataLoop(session, actionElement);
					break;
				case "Schedule":
					action = new Schedule(session, actionElement);
					break;
				case "GitClone":
					action = new GitClone(session, actionElement);
					break;
				case "TensorFlow":
					action = new TensorFlow(session, actionElement);
					break;
				default:
					session.addLogMessage("** Warning **", actionList.item(i).getNodeName(), "Operation not currently supported.");
				}

				if (action != null) {
					action.execute(dataTokens);
					action = null;
				}

				if (stopProcessing)
					break;
			}
			return "";
		} catch (Exception ex) {
			session.addErrorMessage(ex);
			throw ex;
		}
	}

}
