@ECHO OFF
::
:: Script to run the agent directly not as a daemon.
::
SET CONFIG_FILE=config\sfdcConfig.xml
SET LOG4J_CONFIG_FILE=config/log4j.xml
SET LOG_DIR=log
SET LOG_FILE=%LOG_DIR%\email2case.log

SET JAVA_HOME=C:\PROGRA~1\Java\jdk1.7.0_45
SET JAVA_OPTS=-server -Xmx128m -Dlog4j.configuration=file:%LOG4J_CONFIG_FILE%

SET EMAIL2CASE_HOME=%~dp0

ECHO SFDC Email-to-Case Agent v${project.version}
CD %EMAIL2CASE_HOME%
MKDIR %LOG_DIR% 2> NUL
%JAVA_HOME%\bin\java %JAVA_OPTS% -jar sfdc-email-to-case-agent.jar %CONFIG_FILE% >> %LOG_FILE% 2>&1
