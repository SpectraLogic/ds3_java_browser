"%WIX%\bin\heat.exe" dir "..\..\..\..\..\Java" -ag -sfrag -dr "APPLICATIONFOLDER" -cg JavaFilesGroup -var var.JavaPath -out ..\..\..\javaFiles.wxs
powershell -Command "(gc ..\..\..\javaFiles.wxs) -replace '<Component ', '<Component Win64=\"yes\" ' | Out-File ..\..\..\javaFiles.wxs" -encoding UTF8