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
            if (IsPerUserInstalled())
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

        private static bool IsPerUserInstalled()
        {
            var key = RegistryKey.OpenBaseKey(RegistryHive.CurrentUser, RegistryView.Registry64)
                .OpenSubKey("Software\\Spectra Logic\\BlackPearl Eon Browser");

            return key != null;
        }

        private static bool IsAllUsersInstalled()
        {
            var key = RegistryKey.OpenBaseKey(RegistryHive.LocalMachine, RegistryView.Registry64)
                .OpenSubKey("Software\\Spectra Logic\\BlackPearl Eon Browser");

            return key != null;
        }

        private static bool IsPerUserInstall(Session session)
        {
            var allUsers = session["ALLUSERS"];
            return allUsers.Equals("");
        }

        private static bool IsAllUsersInstall(Session session)
        {
            var allUsers = session["ALLUSERS"];
            return allUsers.Equals("1");
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

        [CustomAction]
        public static ActionResult MixInstallCheck(Session session)
        {
            var productName = session["ProductName"];

            if (IsPerUserInstalled() && IsAllUsersInstall(session))
            {
                session.Message(InstallMessage.Error,
                    new Record { FormatString = $"{productName} cannot install due to prexisting per user installation." });
                return ActionResult.Failure;
            }
            else if (IsAllUsersInstalled() && IsPerUserInstall(session))
            {
                session.Message(InstallMessage.Error,
                    new Record { FormatString = $"{productName} cannot install due to prexisting all users installation." });
                return ActionResult.Failure;
            }
            else
            {
                return ActionResult.Success;
            }
        }
    }
}