set SCRIPT_DIR=%~dp0
java -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=256m -Xmx768M -Xss2M -jar "%SCRIPT_DIR%\sbt-launch-0.11.2.jar" %*
