using System;
using System.IO;
using Microsoft.Deployment.WindowsInstaller;
using Microsoft.Win32;

namespace CustomAction
{
    public class CustomActions
    {
        [CustomAction]
        public static ActionResult CustomActions1(Session session)
        {
            session.Log("Begin CustomActions1");

            return ActionResult.Success;
        }

        [CustomAction]
        public static ActionResult DeleteLogFile(Session session)
        {
            if (IsPerUserInstall())
            {
                var userPath = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
                DeleteUserDataFolder(session, userPath);

            }
            else
            {
                var usersPath = Directory.EnumerateDirectories(@"C:\Users");

                foreach (var userPath in usersPath)
                {
                    DeleteUserDataFolder(session, userPath);
                }

            }

            return ActionResult.Success;
        }

        private static bool IsPerUserInstall()
        {
            var key = RegistryKey.OpenBaseKey(RegistryHive.CurrentUser, RegistryView.Registry64)
                .OpenSubKey("Software\\Spectra Logic\\BlackPearl Eon Browser");

            return key != null;
        }

        private static void DeleteUserDataFolder(Session session, string userFolder)
        {
            try
            {
                var path = Path.Combine(userFolder + "\\.dsbrowser");

                if (Directory.Exists(path))
                {
                    Directory.Delete(path, true);
                }
            }
            catch (Exception ex)
            {
                session.Log("Inside DeleteUserDataFolder" + ex);
            }
        }
    }
}
