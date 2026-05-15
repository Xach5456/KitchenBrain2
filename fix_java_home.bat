@echo off
echo Setting JAVA_HOME for Android Studio JBR...
set JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
set PATH=%JAVA_HOME%\bin;%PATH%
echo JAVA_HOME set to: %JAVA_HOME%
echo Testing Java...
java -version
echo.
echo Running Gradle clean...
.\gradlew.bat clean
pause
