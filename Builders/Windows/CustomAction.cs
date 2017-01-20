using System;
usingSystem.Collections.Generic;
usingSystem.Text;
usingMicrosoft.Deployment.WindowsInstaller;
using System.IO;
usingSystem.Windows;

namespaceCustomAction
{
publicclassCustomActions
{
[CustomAction]
publicstaticActionResult CustomAction1(Session session)
{
session.Log("Begin CustomAction1");

returnActionResult.Success;
}

[CustomAction]
publicstaticActionResultDeleteLogFile(Session session)
{
try
{
varfileName = Environment.GetFolderPath(
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
returnActionResult.Success;
}
