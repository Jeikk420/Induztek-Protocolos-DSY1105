@echo off
setlocal
set "JAVA=java.exe"
if defined JAVA_HOME set "JAVA=%JAVA_HOME%\bin\java.exe"
"%JAVA%" %MAVEN_OPTS% "-Dmaven.multiModuleProjectDirectory=%~dp0" -classpath "%~dp0.mvn\wrapper\maven-wrapper.jar" org.apache.maven.wrapper.MavenWrapperMain %*
exit /b %ERRORLEVEL%
