using System.Diagnostics;
using Microsoft.Win32;

namespace DsbExecutable
{
    public static class Program
    {
        public static void Main()
        {
            const string root = @"HKEY_CURRENT_USER\Software\Spectra Logic\BlackPearl Eon Browser";
            var installFolderValue = (string)Registry.GetValue(root, "InstallFolder", null);
            var versionValue = (string)Registry.GetValue(root, "Version", null);

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

                sw.WriteLine(@"echo off");
                sw.WriteLine(@"cd " + "\"" + installFolderValue + "\"");
                sw.WriteLine(@"set VERSION=" + versionValue);
                sw.WriteLine(@"set JAVA_HOME=Java\jre1.8.0_121");
                sw.WriteLine(@"set JAVA_EXE=%JAVA_HOME%\bin\javaw.exe");
                sw.WriteLine(@"set CLASSPATH=distributions\dsb-gui-%VERSION%\lib\*");
                sw.WriteLine("START %JAVA_EXE% -classpath %CLASSPATH% com.spectralogic.dsbrowser.gui.Main");
            }
        }
    }
}
