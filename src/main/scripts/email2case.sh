#!/bin/bash
#
# Script to run the agent directly not as a daemon.
#
CONFIG_FILE=config/sfdcConfig.xml
LOG4J_CONFIG_FILE=config/log4j.xml
LOG_DIR=log
LOG_FILE=$LOG_DIR/email2case.log

JAVA_HOME=/usr/java/jdk1.7.0_51

JAVA_OPTS="-server \
           -Xmx128m \
           -Dlog4j.configuration=file:$LOG4J_CONFIG_FILE \
           -Dlog4j.debug \
           -Dmail.imap.connectiontimeout=900000 \
           -Dmail.imap.timeout=750000 \
           -Dmail.smtp.connectiontimeout=900000 \
           -Dmail.smtp.timeout=750000 \
           -Dmail.smtp.ssl.enable=true \
           -Dmail.smtp.starttls.enable=true"

EMAIL2CASE_HOME=$(dirname "${0}")/..

echo SFDC Email-to-Case Agent v${project.version}
cd $EMAIL2CASE_HOME
mkdir $LOG_DIR 2> /dev/null
$JAVA_HOME/bin/java $JAVA_OPTS -jar sfdc-email-to-case-agent.jar $CONFIG_FILE >> $LOG_FILE 2>&1
