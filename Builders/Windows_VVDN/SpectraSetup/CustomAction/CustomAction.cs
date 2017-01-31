using System;
using System.Collections.Generic;
using System.Text;
using Microsoft.Deployment.WindowsInstaller;
using System.IO;
using System.Windows;
using System.Diagnostics;
using System.Linq;

namespace CustomAction
{
    public class CustomActions
    {
        [CustomAction]
        public static ActionResult CustomAction1(Session session)
        {
            session.Log("Begin CustomAction1");

            return ActionResult.Success;
        }

        [CustomAction]
        public static ActionResult DeleteLogFile(Session session)
        {
            try
            {
                var fileName = Environment.GetFolderPath(
             Environment.SpecialFolder.UserProfile);

                string path = System.IO.Path.Combine(fileName + "\\.dsbrowser");

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
