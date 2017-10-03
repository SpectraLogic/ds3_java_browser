using System;
using System.Diagnostics;
using Microsoft.Win32;

namespace DsbExecutable
{
    public static class Program
    {
        private enum Status
        {
            NotFound,
            Ok
        }
        public static void Main()
        {
            string installFolderValue;
            string versionValue;

            var status = GetRegKeys(RegistryHive.CurrentUser, out installFolderValue, out versionValue);
            if (status == Status.NotFound)
            {
                status = GetRegKeys(RegistryHive.LocalMachine, out installFolderValue, out versionValue);
                if (status == Status.NotFound)
                {
                    throw new Exception("Failed to get registry keys for current user and local machine");
                }
            }
            var p = new Process();
            var info = new ProcessStartInfo
            {
                WindowStyle = ProcessWindowStyle.Hidden,
                FileName = "cmd.exe",
                RedirectStandardInput = true,
                UseShellExecute = false
            };

            p.StartInfo = info;
            p.Start();

            using (var sw = p.StandardInput)
            {
                if (!sw.BaseStream.CanWrite) return;

                sw.WriteLine(@"cd " + "\"" + installFolderValue + "\"");
                sw.WriteLine(@"set VERSION=" + versionValue);
                sw.WriteLine(@"set JAVA_HOME=Java\jre1.8.0_144");
                sw.WriteLine(@"set JAVA_EXE=%JAVA_HOME%\bin\javaw.exe");
                sw.WriteLine(@"set CLASSPATH=distributions\dsb-gui-%VERSION%\lib\*");
                sw.WriteLine("START %JAVA_EXE% -classpath %CLASSPATH% com.spectralogic.dsbrowser.gui.Main");
            }
        }

        private static Status GetRegKeys(RegistryHive registryHive, out string installFolderValue, out string versionValue)
        {
            var key = RegistryKey.OpenBaseKey(registryHive, RegistryView.Registry64)
                .OpenSubKey("Software\\Spectra Logic\\BlackPearl Eon Browser");

            installFolderValue = (string)key?.GetValue("InstallFolder", null);
            versionValue = (string)key?.GetValue("Version", null);

            return string.IsNullOrWhiteSpace(installFolderValue) ? Status.NotFound : Status.Ok;
        }
    }
}