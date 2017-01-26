using System;
using System.IO;
using Microsoft.Deployment.WindowsInstaller;

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
            try
            {
                var fileName = Environment.GetFolderPath(
                    Environment.SpecialFolder.UserProfile);

                var path = Path.Combine(fileName + "\\.dsbrowser");

                if (Directory.Exists(path))
                {
                    Directory.Delete(path, true);
                }
            }
            catch (Exception ex)
            {
                session.Log("Inside DeleteLogFile" + ex);
            }
            return ActionResult.Success;
        }
    }
}
