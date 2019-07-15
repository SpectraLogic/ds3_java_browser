set "currentWorkingDir=%cd%"
cd ..\..\..\..\..\..\..
call .\gradlew clean dsb-gui:copyDependencies

call "C:\Program Files (x86)\Launch4j\launch4jc.exe" dsb-gui\launch4j.xml

cd %currentWorkingDir%
