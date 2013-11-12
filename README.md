## Sforce Email-to-Case Version ${project.version}

### Important Note on Email To Case as a Service

Please note that as of the Spring '09 release we now offer Email To Case as
a Service.  
This means that in most cases, you no longer need the client software described
on this page; you need only create an On Demand Email To Case inbox and forward
your emails to it.  For information on how to do that, please see 
[this blog post](http://blogs.salesforce.com/support/2009/02/new-in-spring-1.html).

You may still need the client software if you expect to be taking attachments
larger than 10Mb, or if you need to export attachments to a local server.  
If this is not the case for you, then you will likely be better served using
Email To Case as a Service.

### About

This Email-to-Case Agent source code is a toolkit that pulls emails from
your mail server and uses the sforce API to create new cases or append to
an existing case.  
It currently supports IMAP servers.

Customers can use the source code to extend the Email Agent to meet additional
requirements.

The Email-to-Case Toolkit is subject to the terms of Salesforce.com's existing
Support policies. Salesforce.com will provide support for only the most current
version of the Email-to-Case Toolkit, including support for the most current
version of the Toolkit's backing API.  
If the source code is changed by a customer to meet additional requirements,
Salesforce.com is no longer responsible for providing support.

### Download

Click [EmailAgent.zip](http://downloads.developerforce.com/website/EmailAgent.zip)
to download.

### Getting Started

- Make sure you have JDK 1.7.0 or above installed.

- Make sure you have a test email account, and test Salesforce Service and
  Support organization.  
  Developer Edition accounts are free, and a great place to test.

- Enable Email-to-Case in your Salesforce Service and Support account.

- Download the Zip file and extract to the local directory of your choice.  
  We will refer to your local directory as **LOCAL** from now on.

- The following directory structure will be created:
    - **LOCAL**\EmailAgent\ : Contains configs and main, src and doc JARs
    - **LOCAL**\EmailAgent\lib\ : Supporting jar files

- In the **LOCAL**\EmailAgent\config\ directory you will need to configure
  the **sfdcConfig.xml** file to connect to Salesforce.  
  Details on this are below.

- Also in the **LOCAL**\EmailAgent\config\ directory, you will need to edit
  **email2case.xml** to configure connections to your mail servers.  
  Details on this are below.

- To run the Email Agent now, on _win32_ you can use the supplied bat file,
  **email2case.bat**, and the agent will start polling your mail server.  
  To invoke the agent from the command line, use the following command from the
  **LOCAL**\EmailAgent\ directory:
  ```bash
  java -Dlog4j.configuration=file:config/log4j.xml -jar sfdc-email-to-case-agent.jar config/sfdcConfig.xml
  ```

- While the agent is running, it will log messages to the console and to a text
  file in the **LOCAL**\EmailAgent\log\ directory called **SalesforceAgent.log**.  
  Settings for logging can be set in **log4j.xml** file in the same directory.  
  For instance, you can turn off logging to the console entirely by commenting
  out the **CONSOLE** section in this file.  
  It is also possible to change the file name for the log file in
  the **LOGFILE** section.  
  For more information on how to customize logging, consult the
  [Log4J documentation](http://logging.apache.org/log4j/docs/documentation.html)

### Configuration:
email2case.xml and sfdcConfig.xml are both simple XML configuration files.

#### In the email2case.xml configuration file:
```
URL               - Name of the mail server to connect with.

PORT              - The port to connect to on the Mail Server - 
                    optional Default port 143 will be used if not provided.

PROTOCOL          - IMAP, and may support others in the future.

USERNAME          - Name of the user that will log in to the mail server.
                    Typically, the name of the email account, like platinum
                    support.

PASSWORD          - Password to authenticate the user against the mail server.

ENCRYPTEDPASSWORD - If you are using password encryption, this is the encrypted
                    string.
                    (Note: you cannot specify both PASSWORD and 
                    ENCRYPTEDPASSWORD.)

ENCRYPTIONKEYFILE - If you are using password encryption, this is the file
                    containing the key used for encryption & decryption.
                    Required if using ENCRYPTEDPASSWORD

INTERVAL          - How often (in minutes) should the agent poll the mail server
                    for new messages.
                    This must be an integer greater than or equal to 1.

INBOX             - Name of the folder to look for new messages in

READBOX           - Name of the folder to move messages to after they have been 
                    processed.

ERRORBOX          - Name of the folder to move messages to in the event of
                    an error.
                    If the agent cannot successfully execute a transaction with
                    the sfdc server, messages will be moved to this folder
                    so that manual action can be taken if necessary
                    (such as requeueing.)
```
> **A note about folder names:**  
> If you want to nest folder names, be sure
> to either use the delimiter that is supported by your mail server for
> separating folders (often '/' or '.') and the agent will convert to the
> correct delimiter supported by your mail server at runtime.

##### Example #1
```xml
<configFile>
    <server1>
        <url>exchange.company.com</url>
        <protocol>imap</protocol>
        <userName>mailman</userName>
        <password>passwrd</password>
        <interval>10</interval>
        <inbox>testInbox</inbox>
        <readbox>testInbox.testNestedProcessing</readbox>
        <errorbox>testInbox.testNestedError</errorbox>
    </server1>
</configFile>
```

##### Example #2

Note that the email agent can poll multiple email inboxes.  
Here is an example of how to configure it to poll two email inboxes:
```xml
<configFile>
    <server1>
        <url>exchange.company.com</url>
        <protocol>imap</protocol>
        <userName>mailman_1</userName>
        <password>passwrd</password>
        <interval>10</interval>
        <inbox>testInbox_1</inbox>
        <readbox>testInbox.testNestedProcessing</readbox>
        <errorbox>testInbox.testNestedError</errorbox>
    </server1>

    <server2>
        <url>exchange.company.com</url>
        <protocol>imap</protocol>
        <userName>mailman_2</userName>
        <password>passwrd</password>
        <interval>10</interval>
        <inbox>testInbox_2</inbox>
        <readbox>testInbox.testNestedProcessing</readbox>
        <errorbox>testInbox.testNestedError</errorbox>
    </server2>
</configFile>
```

##### Example #3 (encrypted password, with one server)
And here is how you would configure a server block with an encrypted password:
```xml
<configFile>
    <server1>
        <url>exchange.company.com</url>
        <protocol>imap</protocol>
        <userName>mailman</userName>
        <encryptedPassword>889c5c0e87b66bea7ca1ad88d7f2a9e1</encryptedPassword>
        <encryptionKeyFile>config/sample.key</encryptionKeyFile>
        <interval>10</interval>
        <inbox>testInbox</inbox>
        <readbox>testInbox.testNestedProcessing</readbox>
        <errorbox>testInbox.testNestedError</errorbox>
    </server1>
</configFile>
```

#### In the sfdcConfig.xml file:

##### Salesforce Connectivity
```
URL                          - URL of the Salesforce web services endpoint.
                               Most customers do not need to change this

USERNAME                     - Username of the Salesforce user who will create
                               cases

ENCRYPTEDPASSWORD            - If you are using password encryption, this is 
                               the encrypted string
                               (Note: you cannot specify both PASSWORD and
                               ENCRYPTEDPASSWORD).

ENCRYPTIONKEYFILE            - If you are using password encryption, this is
                               the file containing the key used for encryption 
                               & decryption. Required if using ENCRYPTEDPASSWORD

LOGINREFRESH                 - How often should the code relogin to refresh
                               the Salesforce session, in minutes.

TIMEOUT                      - The timeout to specify for the SOAP binding,
                               in seconds. Default is 600.

com.sforce.mail.EmailService - Pointer to the email2Case configuration file.
```

##### Notification Processing
```
NOTIFYEMAIL  - Email address of person to send notification to in event of a
               problem.
               To send to multiple recipients, separate addresses by commas.

FROM         - Sender Address of above.

HOST         - SMTP Host for Email, or if using Notification extensions,
               this can be any other type of host you need.

PORT         - SMTP Port for above host (optional).
               Default port 25 will be used if not provided.

USER         - For SMTP, this is the user needed for SMTP to authenticate.

PASSWORD     - For authentication, if needed.

SERVICE      - The class to use for invoking notifications.
               The provided class, com.sforce.mail.SMTPNotification, is for 
               sending Email notifications via SMTP.

               Another class is also provided for use with SMTP servers that
               require authentication (e.g. Yahoo, Hosted Mail Providers).
               This class, com.sforce.mail.SMTPNotificationAuth, does require
               the user and password parameters to be provided.

               The architecture is designed such that another class could be
               used in its place for other protocols as desired, such as SMNP
               or JMX.
               To do this, use subclass com.sforce.mail.Notification.
```

##### Large Attachment Processing

Salesforce has a limit on the size of attachments for any single case.  
When an attachment exceeds this limit, the creation of a case for the email 
fails and an error is generated.  
To prevent this, and to manage the storage of large attachments, the files can 
be stripped from the email they are attached to, and stored on a file system 
that you specify.  
Here are the settings that you must configure to activate this optional feature.
```
largeAttachmentDirectory  - This is the high level directory where you want to
                            store the attachments.
                            Subdirectories will be created each day 
                            an attachment is processed.

largeAttachmentURLPrefix  - This is a url which will prefix a unique filename
                            stored in a proxy file that points to the real file
                            so that you can link to the file from Salesforce

largeAttachmentSize       - Specified in MB, this is the threshold at which
                            the agent will strip attachments and copy them to 
                            disk.
```
##### Example #1 (with authenticated SMTP)
```xml
<configFile>
    <sfdcLogin>
        <url>https://login.salesforce.com/services/Soap/u/29.0</url>
        <userName>TestUser@Company.com</userName>
        <password>MyPassword</password>
        <loginRefresh>30</loginRefresh>
        <timeout>600</timeout>
    </sfdcLogin>
    <notify>
        <notifyEmail>admin@your_company.com, E2CSupport@your_company.com</notifyEmail>
        <from>sample_user@your_company.com</from>
        <host>smtp.mail.your_company.com</host>
        <port>25</port>
        <user>sample_user</user>
        <password>123456</password>
        <service>com.sforce.mail.SMTPNotificationAuth</service>
    </notify>
    <attachments>
        <largeAttachmentDirectory>\\nfs-001\west\cases\</largeAttachmentDirectory>
        <largeAttachmentURLPrefix>file:\\nfs-001\west\cases\</largeAttachmentURLPrefix>
        <largeAttachmentSize>5</largeAttachmentSize>
    </attachments>
    <services>
        <com.sforce.mail.EmailService>C:\\EmailAgent\\config\\email2case.xml</com.sforce.mail.EmailService>
    </services>
</configFile>
```

##### Example #2 (without authenticated SMTP)
```xml
<configFile>
    <sfdcLogin>
        <url>https://login.salesforce.com/services/Soap/u/29.0</url>
        <userName>TestUser@Company.com</userName>
        <password>MyPassword</password>
        <loginRefresh>30</loginRefresh>
        <timeout>600</timeout>
    </sfdcLogin>
    <notify>
        <notifyEmail>admin@your_company.com, E2CSupport@your_company.com</notifyEmail>
        <from>sample_user@your_company.com</from>
        <host>smtp.mail.your_company.com</host>
        <port>25</port>
        <user>sample_user</user>
        <password>123456</password>
        <service>com.sforce.mail.SMTPNotification</service>
    </notify>
    <attachments>
        <largeAttachmentDirectory>\\nfs-001\west\cases\</largeAttachmentDirectory>
        <largeAttachmentURLPrefix>file:\\nfs-001\west\cases\</largeAttachmentURLPrefix>
        <largeAttachmentSize>5</largeAttachmentSize>
    </attachments>
    <services>
        <com.sforce.mail.EmailService>C:\\EmailAgent\\config\\email2case.xml</com.sforce.mail.EmailService>
    </services>
</configFile>
```

##### Example #3 (when using an encrypted password to connect to Salesforce)
```xml
<configFile>
    ...
    <sfdcLogin>
        <url>https://login.salesforce.com/services/Soap/u/29.0</url>
        <userName>TestUser@Company.com</userName>
        <encryptedPassword>889c5c0e87b66bea7ca1ad88d7f2a9e1</encryptedPassword>
        <encryptionKeyFile>config/sample.key</encryptionKeyFile>
        <loginRefresh>30</loginRefresh>
        <timeout>600</timeout>
    </sfdcLogin>
    ...
</configFile>
```

### Guide to Using Password Encryption

If you would prefer not to store your passwords in plaintext in the config files,
you can use built-in functionality for using an encrypted password. This can be
used with both the <sfdcLogin> config section (for connection to Salesforce) and
with the <server1> configs used to configure the IMAP servers. To use encryption,
here are the additional steps required:

1.  Generate a key file. An example key file (sample.key) is provided.  
    To make your own key, run the following command from the command line:
    ```bash
    java -cp <path to sfdc-email-to-case-agent.jar> com.sforce.util.EncryptionUtil -g <seedText>
    ```

    ... where <seedText> is the string of your choice used to generate a random key.

2.  Take the output of this command and store it in your key file.  
    You can use the given **sample.key** if you wish, but we recommend making
    your own.

3.  Encrypt your password. From the command line:
    ```bash
    java -cp <path to sfdc-email-to-case-agent.jar> com.sforce.util.EncryptionUtil -e <password> <path to keyfile>
    ```

    ... and save the output. Note that if you do not pass a keyfile, a default
    key will be used, but this default key cannot be used for decryption,
    so you will later get a decryption error.  
	You must provide a keyfile.

4.  In the relevant configuration file (either **sdfcConfig.xml** or
    **email2Case.xml**), delete the `<password>` configuration element and
    replace it with the encrypted password as `<encryptedPassword>`, and
    specify the key file as `<encryptionKeyFile>`.

If you are having trouble, make certain that the same key file is used for both
encryption and decryption.  
You can test your stored encrypted password with the verify command:
```bash
java -cp <path to sfdc-email-to-case-agent.jar> com.sforce.util.EncryptionUtil -v <encryptedPassword> <expectedValue> <path to keyfile>
```

### Additional Information About this Code

If you need to generate a WSDL with the Email-to-Case API calls in the WSDL, add
`&email=1` to the end of the WSDL generation URL in the application:

- https://na1.salesforce.com/soap/wsdl.jsp?email=1 (Partner WSDL)
- https://na1.salesforce.com/soap/wsdl.jsp?type=*&email=1 (Enterprise WSDL)

There is an API call to support Email-to-Case called handleEmailMessage, which
takes an array of HandledEmailMessage.  HandledEmailMessage contains the email
body, and any attachments associate with the email. The new API call will
either create a new case, or append the email to an existing case based on the
presence of a key (threadID) in the subject, or in the body of the message.  
The key is added to an outbound email when Email-to-Case is enabled.

This code is written in Java, and requires JDK 1.7.0 or greater. It utilizes
the WSC SOAP stack.

### Daemon Script

Here is a script to run the tool as a daemon on *nix.

This script has been tested on Red Hat and can be easily changed to run on other
*nix flavors.

```bash
#!/bin/bash
#
# Startup script for SFDC Email-to-Case Agent
#
# chkconfig: - 86 15
# description: Email to Case Agent for SalesForce.com
# processname: email2case
#

EMAIL2CASE_USER=a_user
EMAIL2CASE_HOME=/home/$EMAIL2CASE_USER/EmailAgent

CONFIG_FILE=$EMAIL2CASE_HOME/config/sfdcConfig.xml
LOG4J_CONFIG_FILE=$EMAIL2CASE_HOME/config/log4j.xml
LOG_DIR=$EMAIL2CASE_HOME/log
LOG_FILE=$LOG_DIR/email2case.log
PID_FILE=$EMAIL2CASE_HOME/run/email2case.pid

JAVA_HOME=/usr/java/jdk1.7.0_45
JAVA_OPTS='-server -Xmx128m -Dlog4j.configuration=file:$LOG4J_CONFIG_FILE'

# Source function library.
. /etc/rc.d/init.d/functions

# Get config.
. /etc/sysconfig/network

# Check that networking is up.
[ "${NETWORKING}" = "no" ] && exit 0

start() {
    echo "Starting SFDC Email-to-Case Agent:"
    cd $EMAIL2CASE_HOME
    su $EMAIL2CASE_USER -c "mkdir $LOG_DIR 2> /dev/null; nohup $JAVA_HOME/bin/java $JAVA_OPTS -jar sfdc-email-to-case-agent.jar $CONFIG_FILE >> $LOG_FILE 2>&1 & echo \$! > $PID_FILE"
    PID="$(<$PID_FILE)"
    echo "PID: $PID"
    echo "Done!"
    RETVAL=$?
    echo
}

status() {
    if [ -f $PID_FILE ]
    then
        PID="$(<$PID_FILE)"
        ps -p $PID 2>&1 > /dev/null
        STATUS=$?
        if [ $STATUS -eq 1 ]
        then
            echo "SFDC Email-to-Case Agent is NOT running"
        else
            echo "SFDC Email-to-Case Agent is running"
            echo "PID: $PID"
        fi
    else
        echo "Error: PID file not found"
    fi
    RETVAL=$?
    echo
}

stop() {
    if [ -f $PID_FILE ]
    then
        echo "Shutting down SFDC Email-to-Case Agent:"
        PID="$(<$PID_FILE)"
        ps -p $PID 2>&1 > /dev/null
        STATUS=$?
        if [ $STATUS -eq 0 ]
        then
            kill $PID
            rm $PID_FILE
            echo "Done!"
        else
            echo "Error: SFDC Email-to-Case Agent is NOT running"
        fi
    else
        echo "Error: PID file not found"
    fi
    RETVAL=$?
    echo
}

# See how we were called
case "$1" in
  start)
        start
        ;;
  stop)
        stop
        ;;
  status)
        status
        ;;
  restart)
        stop
        sleep 10
        start
        ;;
  *)
        echo "Usage: $0 {start|stop|status|restart}"
esac

exit 0
```

#### Installation

- Copy the script into a file named say 'sfdc-email-to-case-agent' in /etc/init.d/

- Modify the following variable to fit your environment:
  * JAVA_HOME
  * JAVA_OPTS
  * EMAIL2CASE_USER
  * EMAIL2CASE_HOME

- Make the script executable
  ```bash
  chmod a+x /etc/init.d/sfdc-email-to-case-agent
  ```

- Register the service
  ```bash
  /sbin/chkconfig --add sfdc-email-to-case-agent
  ```

- Set it to start at level 3, 4 and 5
  ```bash
  /sbin/chkconfig --level 345 sfdc-email-to-case-agent on
  ```
