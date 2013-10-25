Sforce Email to Case Version 1.08

This EmailToCase Agent source code is a toolkit that pulls emails from your mail
server and uses the sforce API to create new cases or append to an existing case.
It currently supports IMAP servers.

Customers can use the source code to extend the Email Agent to meet additional
requirements.

The Email To Case Toolkit is subject to the terms of Salesforce.com's existing
Support policies. Salesforce.com will provide support for only the most current
version of the Email To Case Toolkit, including support for the most current
version of the Toolkit's backing API.  If the source code is changed by a customer
to meet additional requirements, Salesforce.com is no longer responsible to provide
support.

Getting started:

- Make sure you have JDK 1.6.0 or above installed.
- Make sure you have a test email account, and test Salesforce Service and Support
  organization to test with.  Developer Edition accounts are free, and a great place to test.
- Enable EmailToCase in your Salesforce Service and Support account.
- Download the Zip file and extract to the local directory of your choice. We will
  refer to your local directory as $Local from now on.
- The following directory structure will be created:
      $Local\EmailAgent\       : Contains configs and main, src and doc JARs
      $Local\EmailAgent\lib\   : Supporting jar files

- In the $Local\EmailAgent\ directory you will need to configure
  the sfdcConfig.xml file to connect to salesforce.com, details on this are below.
- Also in the $Local\EmailAgent\ directory you will need to edit
  email2case.xml to configure connections to your mail servers, details on this are below.
- To run the Email Agent now, on win32 you can use the supplied bat file
  email2case.bat and the agent will start polling your mail server.  To invoke
  the agent from the command line, use the following command from the
  $Local\EmailAgent\ directory :
     java -jar Email2Case.jar sfdcConfig.xml log4j.properties
- While the agent is running, it will log messages to the console and to a text
  file in the $Local\EmailAgent\ directory called SalesforceAgent.log.
  Settings for logging can be set in log4j.properties file in the same directory.
  For instance, you can turn off logging to the console entirely by commenting
  out the CONSOLE section in this file.  It is also possible to change the file
  name for the log file in the LOGFILE section. for more information on how to
  customize logging, consult the log4j documentation at
  http://logging.apache.org/log4j/docs/documentation.html

Configuration:
email2case.xml and sfdcConfig.xml are both simple XML configuration files.
********************************************************************************
In the email2case.xml configuration file:
********************************************************************************
URL      - Name of the mail server to connect with.
PORT     - The port to connect to on the Mail Server - optional Default port 143 will
		   be used if not provided.
PROTOCOL - IMAP, and may support others in the future.
USERNAME - Name of the user that will login to the mail server.
           Typically, the name of the email account, like platinumsupport
PASSWORD - Password to authenticate the user against the mail server
INTERVAL - How often (in minutes) should the agent poll the mail server for new messages.
           This must be an integer greater than or equal to 1.
INBOX    - Name of the folder to look for new messages in
READBOX  - Name of the folder to move messages to after they have been processed.
ERRORBOX - Name of the folder to move messages to in the event of an error.  If
           the agent cannot successfully execute a transaction with the sfdc
           server, messages will be moved to this folder so that manual action
           can be taken if necessary (requeueing etc...).

           A note about folder names:  If you want to nest folder names, be sure
           to either use the delimiter that is supported by your mail server for
           separating folders(often '/' or '.' or you can always use '.' and the
           agent will convert to the correct delimiter supported by your mail
           server at runtime.
Example #1:
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

Note that the email agent can poll multiple email inboxes.  Here is an example of how
to configure it to poll two email inboxes.

Example #2:
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

********************************************************************************

********************************************************************************
In the sfdcConfig.xml file:
********************************************************************************
Salesforce.com Connectivity
--------------------------------------------------------------------------------
URL          - URL of the Salesforce web services endpoint.  Most customers do
               not need to change this
USERNAME     - Username of the Salesforce user who will create cases
PASSWORD     - Password of the Salesforce user
LOGINREFRESH - How often should the code relogin to refresh the Salesforce session, in minutes.
TIMEOUT      - The timeout to specify for the SOAP binding, in seconds.  Default is 600.
com.sforce.mail.EmailService - Pointer to the email2Case configuration file.

Notification Processing:
--------------------------------------------------------------------------------
NOTIFYEMAIL  - Email address of person to send notification to in event of a
               problem.  To send to multiple recipients, separate addresses by
               commas.
FROM         - Sender Address of above.
HOST         - SMTP Host for Email, or is using Notification extensions this can
               be any other type of host you need.
PORT         - SMTP Port for above host - optional. Default port 25 will be used
               if not provided.
USER         - For SMTP, this is the user needed for SMTP to authenticate.
PASSWORD     - For authentication, if needed.
SERVICE      - The class to use for invoking notifications.  The provided class,
               com.sforce.mail.SMTPNotification, is for sending Email
               notifications via SMTP.

               Another class is also provided for use with SMTP servers that
               require authentication (e.g. Yahoo, Hosted Mail Providers).  This Class,
               com.sforce.mail.SMTPNotificationAuth does require the user and
               password parameters to be provided

               The architecture is designed such that another class could be used
               in its place for other protocols as  desired SMNP, JMX etc...
               To do this, subclass com.sforce.mail.Notification.

Large Attachment Processing:
--------------------------------------------------------------------------------
Salesforce.com has a limit on the size of attachments for any single case.  When an
attachment exceeds this limit, the creation of a case for the email fails and an
error is generated.  To prevent this from happening, and to manage the storage of
large attachments, the files can be stripped from the email they are attached to,
and stored on a file system that you specify.  Here are the settings that you
must configure to activate this optional feature.

largeAttachmentDirectory  - This is the high level directory where you want to store
                            the attachments.  Subdirectories will be created each day
                            an attachment is processed.
largeAttachmentURLPrefix  - This is a url which will prefix a unique filename
                            stored in a proxy file that points to the real file
                            so that you can link to the file from the Salesforce
                            application
largeAttachmentSize       - Specified in MB, this is the threshold at which the agent
                            will strip attachments and copy them to disk.

Example #1 with authenticated SMTP:
<configFile>
    <sfdcLogin>
        <url>https://www.salesforce.com/services/Soap/u/9.0</url>
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
        <com.sforce.mail.EmailService>C:\\EmailAgent\\email2case.xml</com.sforce.mail.EmailService>
    </services>
</configFile>


Example #2 without authenticated SMTP:
<configFile>
    <sfdcLogin>
        <url>https://www.salesforce.com/services/Soap/u/9.0</url>
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
        <com.sforce.mail.EmailService>C:\\EmailAgent\\email2case.xml</com.sforce.mail.EmailService>
    </services>
</configFile>
********************************************************************************

Additional information about this code:

If you need to generate a WSDL with the EmailToCase API calls in the WSDL, add
"&email=1" to the end of the WSDL generation URL in the application.

https://na1.salesforce.com/soap/wsdl.jsp?email=1 (Partner WSDL)
https://na1.salesforce.com/soap/wsdl.jsp?type=*&email=1 (Enterprise WSDL)

There is an API call to support EmailToCase called handleEmailMessage, which
takes an array of HandledEmailMessage.  HandledEmailMessage contains the email
body, and any attachments associate with the email.  The new API call will
either create a new case, or append the email to an existing case based on the
presence of a key (threadID) in the subject, or in the body of the message.  The key is
added to an outbound email when EmailToCase is enabled.

This code is written in Java, and requires JDK 1.6.0 or greater.  It utilizes
the WSC SOAP stack.
