/**
 *  
 * Copyright (c) 2016 Fannie Mae, All rights reserved.
 * This program and the accompany materials are made available under
 * the terms of the Fannie Mae Open Source Licensing Project available 
 * at https://github.com/FannieMaeOpenSource/ezPIE/wiki/Fannie-Mae-Open-Source-Licensing-Project
 * 
 * ezPIE is a trademark of Fannie Mae
 * 
 */

using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.IO;
using System.Diagnostics;

/**
 * 
 * @author Rick Monson (richard_monson@fanniemae.com, https://www.linkedin.com/in/rick-monson/)
 * @since 2016-08-11
 * 
 */

namespace ScanManager.Common
{
    sealed class RunCommand
    {
        private RunCommand() { }

        public static void Execute(String WorkDirectory, String CommandLine)
        {
            Execute(WorkDirectory, CommandLine, true, 0);
        }

        public static void Execute(String WorkDirectory, String CommandLine, Boolean WaitForExit, int Timeout)
        {
            if (String.IsNullOrEmpty(WorkDirectory)) throw new Exception("No WorkDirectory value specified.");
            if (String.IsNullOrEmpty(CommandLine)) throw new Exception("No CommandLine value specified.");
            if (!Directory.Exists(WorkDirectory)) throw new Exception(String.Format("WorkDirectory {0} does not exist.", WorkDirectory));

            String ConsoleScreenText;
            int ExitCode;

            ProcessStartInfo psi = new ProcessStartInfo(CommandLine);
            psi.CreateNoWindow = true;
            psi.UseShellExecute = false;
            psi.WorkingDirectory = WorkDirectory;
            psi.RedirectStandardOutput = true;
            using (Process pcmd = Process.Start(psi))
            {
                ConsoleScreenText = pcmd.StandardOutput.ReadToEnd();
                if (WaitForExit)
                {
                    if (Timeout > 0) pcmd.WaitForExit(Timeout);
                    else pcmd.WaitForExit();
                }
                ExitCode = pcmd.ExitCode;
                pcmd.Close();
            }
            if (ExitCode > 0)
                throw new Exception(String.Format("Error #{0} running {1}", ExitCode, CommandLine));
        }
    }
}
