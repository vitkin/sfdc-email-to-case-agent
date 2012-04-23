/*
 * Copyright (c) 2005, salesforce.com, inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 *
 *    Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *    following disclaimer.
 *
 *    Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 *    the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *    Neither the name of salesforce.com, inc. nor the names of its contributors may be used to endorse or
 *    promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.sforce.mail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Semaphore;

import javax.mail.AuthenticationFailedException;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;

import org.apache.log4j.Logger;

import com.sforce.SalesforceAgent;
import com.sforce.config.ConfigParameters;
import com.sforce.exception.FailedBindingException;
import com.sforce.exception.InvalidConfigurationException;
import com.sforce.exception.InvalidConfigurationException.ConfigurationExceptionCode;
import com.sforce.soap.partner.wsc.Connector;
import com.sforce.soap.partner.wsc.HandledEmailMessage;
import com.sforce.soap.partner.wsc.LoginResult;
import com.sforce.soap.partner.wsc.PartnerConnection;
import com.sforce.soap.partner.wsc.SaveResult;
import com.sforce.soap.partner.wsc.Error;
import com.sforce.util.EncryptionUtil;

import com.sforce.soap.partner.fault.wsc.ApiFault;
import com.sforce.soap.partner.fault.wsc.ExceptionCode;
import com.sforce.soap.partner.fault.wsc.LoginFault;
import com.sforce.config.ConfigInfo;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

/**
 * @author Echan
 * @author CSpeer
 * @author pdebaty
 * @since 140
 */
public abstract class GenericClient {
    private static final String EMAIL_TO_CASE_NOT_ENABLED = "Email-to-Case is not enabled for your organization!";
    private static final String EMAIL_TO_CASE_ROUTING_INCORRECT = "Routing Addresses are not set up correctly to run Email To Case.";
    private static final String pSHUTTING_DOWN_CONNECTION_TO_MAIL_SYSTEM = "Shutting down connection to mail system.";
    private static final String pNO_DEFAULT_FOLDER = "No default folder";
    private static final String pAGENT_WILL_CONNECT_TO = "Agent will connect to: ";
    private static final String pLOGIN_URL = "LoginURL: ";
    private static final String pPASSWORD_EXPIRED = "Password expired.  Please reset your password, and update the config file.  For long running integrations, consider setting the password never expires flag on for this profile";
    private static final String pLOGIN_REFRESH = "Could not read loginRefresh from config, defaulting to 25 minutes.";
    private static final String pTIMEOUT = "Could not read timeout from config, defaulting to 2 minutes.";
    private static final String pSUCCESS = "Success";
    private static final String pERROR = "Error";
    private static final String pMESSAGES = " messages";
    private static final String pPROCESSING = "   processing ";
    private static final String BAD_MSGS_FOUND = "Unable to process X message(s).";
    public static final int defaultRefresh = 25;
    public static final int defaultTimeout = 2;

    private static Hashtable<String, Integer> hashServiceStateTable = new Hashtable<String, Integer>(5);

    private LoginCredentials loginCredentials;
    private PartnerConnection conn;
    private String inboxName = "INBOX";
    private String readboxName = null;
    private String errorBoxName = null;
    private long nextLogin = 0;
    private long sleepInterval = 0;
    private int sleepIntervalStep = 1;
    private int loginRefresh = defaultRefresh;
    private int sfdcTimeout = defaultTimeout;

    private static final char FOLDER_DELIM = '.';
    private static final int SFDC_SERVICE_RETRY_COUNT = 3;
    private static final int MAX_THREADS_PER_SERVICE = 2;
    private static int iShutdownReason = 0;

    public static final int SHUTDOWN_EMAIL_TO_CASE_NOT_ENABLED = 1;
    public static final int SHUTDOWN_EMAIL_TO_CASE_NOT_CONFIGURED = 2;
    public static final int SHUTDOWN_EMAIL_TO_CASE_MESSAGING_ERROR = 3;

    protected static String ATTACH_DIR = SalesforceAgent.GLOBAL_CONFIG.get(ConfigParameters.pATTACH, ConfigParameters.pATTACH_DIR);

    private final Semaphore mutex = new Semaphore(1);

    // We only want to log login debug info the first time through
    private static boolean firstTime = true;

    //Logging
    static Logger logger = Logger.getLogger(GenericClient.class.getName());


    /**
     * @param loginCredentials
     */
    protected GenericClient(LoginCredentials loginCredentials) {
        this.loginCredentials = loginCredentials;
        try {
        	
        	// Check for an encrypted password in the SFDC section
        	handlePasswordDecryption(SalesforceAgent.GLOBAL_CONFIG, ConfigParameters.pLOGIN);
        	
            conn = this.getConnection();
        } catch (ConnectionException e) {
            logger.error("Failed to bind to SFDC service on initialization.\n" + e.getMessage());
            SalesforceAgent.processNotification(Notification.SFDC_SERVICE_DOWN, e.getMessage(), Notification.SEVERITY_ERROR);
        } catch (InvalidConfigurationException e) {
            logger.error("Failed to configure SFDC connection.\n" + e.getMessage());
            SalesforceAgent.processNotification(Notification.SFDC_SERVICE_DOWN, e.getMessage(), Notification.SEVERITY_ERROR);
        } 
    }

    /**
     * This constructor is only for testing purposes
     */
    protected GenericClient() {

    }

    private boolean needNewSession() {
        if (conn == null) return true;

        Calendar now = Calendar.getInstance();
        long currentTime = now.getTimeInMillis();
        if (nextLogin < currentTime) {
            return true;
        } else {
            return false;
        }
    }

    private void forceNewSession() {
       nextLogin = -1;
       sleepInterval = 0;
       sleepIntervalStep = 1;
    }

    /**
     * @return binding the SoapBindingStub to gain access to SFDC service
     * @throws FailedBindingException
     * This was made protected for testing purposes.
     */
    protected PartnerConnection getConnection() throws ConnectionException {
        int iAttempts = 0;
        boolean bSuccess = false;

        if (needNewSession()) {
            while (!bSuccess && iAttempts < SFDC_SERVICE_RETRY_COUNT ) {
                try {
                    login();
                    bSuccess = true;
                } catch (ConnectionException e) {
                    logger.error("Failed to connect to SFDC service",e);
                    iAttempts++;
                    if (iAttempts < SFDC_SERVICE_RETRY_COUNT ) {
                        int iRemainingAttempts = SFDC_SERVICE_RETRY_COUNT - iAttempts;
                        logger.info("Will try " + iRemainingAttempts + " more time(s).");
                        sleepUntilNextInterval();
                    }
                    if (! bSuccess && iAttempts >= SFDC_SERVICE_RETRY_COUNT ) {
                        forceNewSession();
                        throw new ConnectionException("Failed to connect to SFDC service after "+iAttempts+" tries. Aborting!", e);
                    }
                } 
            }
        }


        return this.conn;
    }

    private void sleepUntilNextInterval() {
        sleepInterval += (1000 * 60 * sleepIntervalStep++);
        try {
            Thread.sleep(sleepInterval);
        } catch (InterruptedException e) {
            logger.warn("Sleeper Thread interrupted.");
        }
    }

    /**
     * Handles password decryption for the specified server configuration
     */
    public static void handlePasswordDecryption(ConfigInfo config, String serverConfigurationName) throws InvalidConfigurationException {
        
        String password = config.get(serverConfigurationName, ConfigParameters.pPASSWORD);
        String encryptedPassword = config.get(serverConfigurationName, ConfigParameters.pENCRYPTED_PASSWORD);
        String encryptionKeyFile = config.get(serverConfigurationName, ConfigParameters.pENCRYPTION_KEY_FILE);

        // Check for encrypted password. For clarity, we allow either <password> or <encryptedPassword,encryptionKeyFile> to be specified,
        // but no other combination
        if(encryptedPassword != null) {

        	if(password != null) {
        		throw new InvalidConfigurationException(InvalidConfigurationException.ConfigurationExceptionCode.CRYPTO_PASSWORD_COLLISION);
        	}
        	
        	if( encryptionKeyFile == null) {
        		throw new InvalidConfigurationException(InvalidConfigurationException.ConfigurationExceptionCode.CRYPTO_KEY_MISSING);
        	}
        	
        	try {
        		// Attempt the decryption
            	EncryptionUtil decrypter = new EncryptionUtil();
        		decrypter.setCipherKeyFromFilePath(encryptionKeyFile);
        		String decryptedPassword = decrypter.decryptString(encryptedPassword);
        		
        		// Store the newly decrypted password
        		config.put(serverConfigurationName, ConfigParameters.pPASSWORD, decryptedPassword);
        		
        	} catch (IOException e) {
        		throw new InvalidConfigurationException(InvalidConfigurationException.ConfigurationExceptionCode.CRYPTO_KEY_INITIALIZATION_FAILURE, e);
        	} catch (GeneralSecurityException e) {
        		throw new InvalidConfigurationException(InvalidConfigurationException.ConfigurationExceptionCode.CRYPTO_DECRYPTION_FAILURE, e);
        	}
        	
        	
        }
        
    }
    

    
    /**
     * Connects to the Salesforce system as needed, maintaining a live connection until
     * the specified timeout has elapsed. Kanishka made this protected.
     * @throws ConnectionException
     *
     * @throws MalformedURLException
     * @throws ServiceException
     * @throws UnexpectedErrorFault
     * @throws LoginFault
     * @throws RemoteException
     */
    
    protected void login() throws ConnectionException {

        try {
            ConnectorConfig config = new ConnectorConfig();

            // retrieve the sfdc config info from sfdc config file.
            // Note: these are different from loginCredentials, which are the mail server credentials, not sfdc.
            String loginUrl = SalesforceAgent.GLOBAL_CONFIG.get(ConfigParameters.pLOGIN, ConfigParameters.pURL);
            String userName = SalesforceAgent.GLOBAL_CONFIG.get(ConfigParameters.pLOGIN, ConfigParameters.pUSERNAME);
            String password = SalesforceAgent.GLOBAL_CONFIG.get(ConfigParameters.pLOGIN, ConfigParameters.pPASSWORD);
            String refresh = SalesforceAgent.GLOBAL_CONFIG.get(ConfigParameters.pLOGIN, ConfigParameters.pLOGIN_REFRESH);
            String sTimeout = SalesforceAgent.GLOBAL_CONFIG.get(ConfigParameters.pLOGIN, ConfigParameters.pTIMEOUT);


            
            config.setUsername(userName);
            config.setPassword(password);
            config.setAuthEndpoint(loginUrl);
            config.setCompression(false);

            if (refresh != null) {
                loginRefresh = new Integer(refresh).intValue();
            } else {
                logger.info(pLOGIN_REFRESH);
            }

            if (sTimeout != null) {
                try {
                sfdcTimeout = new Integer(sTimeout).intValue();
                } catch ( NumberFormatException nfe ) {
                    logger.error("Invalid argument for timeout setting, defaulted to " + defaultTimeout + " minutes (" + sTimeout +")");
                    SalesforceAgent.GLOBAL_CONFIG.put(ConfigParameters.pLOGIN, "timeout",String.valueOf(defaultTimeout));
                    sfdcTimeout = defaultTimeout;
                }
            } else {
                logger.info(pTIMEOUT);
            }
            config.setConnectionTimeout(this.sfdcTimeout);
            //need to do a manual login to set the proper headers
            config.setManualLogin(true);
            this.conn = Connector.newConnection(config);
            conn.setCallOptions("EmailAgent/" + SalesforceAgent.SALESFORCE_AGENT_VERSION, null, false, null, null);
            conn.setAssignmentRuleHeader(null, true);
            //Note: no email header needed for email2case client because the proper behavior is
            //set by overriden methods in EmailToCaseObject

            if(config.getSessionId() == null) {
                config.setServiceEndpoint(config.getAuthEndpoint());
                LoginResult loginresult = conn.login(config.getUsername(), config.getPassword());
                config.setSessionId(loginresult.getSessionId());
                config.setServiceEndpoint(loginresult.getServerUrl());
            } else if(config.getServiceEndpoint() == null)
                throw new ConnectionException("Please set ServiceEndpoint");

            conn.setSessionHeader(config.getSessionId());

            if (firstTime) {
                logger.info(pLOGIN_URL + config.getAuthEndpoint());
                logger.info(pAGENT_WILL_CONNECT_TO + config.getServiceEndpoint());
            }

            // Setup when the next login will be, to refresh the session
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MINUTE, loginRefresh);
            nextLogin = cal.getTimeInMillis();
            sleepInterval = 0;
            sleepIntervalStep = 1;
            firstTime = false;
        } catch (LoginFault lf) {
            logger.error(lf.getMessage());
            InvalidConfigurationException ice = new InvalidConfigurationException(ConfigurationExceptionCode.SFDC_AUTHENTICATION_ERROR);
            handleConfigurationException(ice);
            if (ice.isFatal()) {
                System.exit(0); // Won't be able to do anything until this is fixed
            }
        }
    }

    protected abstract String getProtocol();

    /**
     * @param errorBox
     */
    public void setErrorbox(String errorBox) {
        if (errorBox == null) return;
        errorBox = errorBox.trim();

        if (errorBox.length() > 0) {
            this.errorBoxName = errorBox;
        }
    }

    public String getErrorBoxName() { return this.errorBoxName; }

    /**
     * @param inbox
     */
    public void setInbox(String inbox) {
        if (inbox == null) return;
        inbox = inbox.trim();

        if (inbox.length() > 0) {
            this.inboxName = inbox;
        }
    }

    public String getInbox() {
        return this.inboxName;
    }

    public String getUrl() {
        return this.loginCredentials.getServerName();
    }

    public String getPort() {
        return String.valueOf(this.loginCredentials.getPort());
    }

    public String getUser() {
        return this.loginCredentials.getUserName();
    }

    /**
     * @param readbox
     */
    public void setReadbox(String readbox) {
        if (readbox == null) {
            this.readboxName = null;
            return;
        }

        readbox = readbox.trim();
        if (readbox.length() > 0) {
            this.readboxName = readbox;
        } else {
            this.readboxName = null;
        }
    }

    public String getReadBoxName() { return this.readboxName; }

    /**
     * Retrieves mail messages from a single mail account, batches these
     * messages and attempts to create cases for these messages in supportForce.
     * If there is an API error, the meesages are flagged and copied to the
     * error folder for IMAP mail systems.
     */
    public void receive() {
        synchronized(this){
            if(! isOkToContinue()){
                return;
            }
        }

        try {
            mutex.acquire();

            Store store = null;
            Folder folder = null;
            Folder inbox = null;
            Folder readbox = null;
            Folder errorbox = null;
            Session session = null;

            try {
                // -- Get hold of the default session --
                Properties props = System.getProperties();
                session = Session.getDefaultInstance(props, null);
                store = connectToMailServer(session);

                if (store != null) {
                    // -- Try to get hold of the default folder --
                    folder = store.getDefaultFolder();
                    if (folder == null) throw new Exception(pNO_DEFAULT_FOLDER);

                    inbox = configureInBox(folder);
                    inbox.open(Folder.READ_WRITE);
                    readbox = configureReadBox(folder);
                    errorbox = configureErrorBox(folder);

                    // -- Get the message wrappers and process them --
                    Message[] msgs = retrieveMessages(inbox, readbox, errorbox, session);
                    int originalIndex = 0;
                    int totalSize = msgs.length;
                    logger.info(pPROCESSING + totalSize + pMESSAGES);

                    // Batch up the messages into groups of 5 to be processed
                    while (totalSize > originalIndex) {
                        int batchSize = Math.min(totalSize - originalIndex, 5);
                        Message[] batchedMsgs = new Message[batchSize];
                        System.arraycopy(msgs, originalIndex, batchedMsgs, 0, batchSize);
                        originalIndex += batchSize;
                        if(! isShutdown()) {
                            handleMessage(batchedMsgs, inbox, readbox, errorbox, session);
                        }
                    }
                }
            } catch (FailedBindingException fbe) {
                handleAPIError(fbe, true, true);
            } catch (InvalidConfigurationException ice) {
                handleConfigurationException(ice);
            } catch (MessagingException me) {
                handleMessagingError(me, inbox, readbox, errorbox, session, false);
            } catch (Exception ex) {
                SalesforceAgent.processNotification(Notification.UNKNOWN_ERROR, ex.getMessage(), Notification.SEVERITY_ERROR);
                logger.error(ex,ex);
            } finally {
                try {
                    if (inbox != null && inbox.isOpen()) inbox.close(true);
                    if (readbox != null && readbox.isOpen()) readbox.close(false);
                    if (errorbox != null && errorbox.isOpen()) errorbox.close(false);
                    if (store != null) store.close();
                } catch (Exception ex2) {
                    logger.error(ex2, ex2);
                    ex2.printStackTrace();
                }
            }
        } catch (InterruptedException ie) {
            logger.error(ie,ie);
        } finally {
            mutex.release();
        }
        decrementClientCounter();
    }

    /**
     * a connection to the mail server is made on every poll instance utilizing the credentials fetched when the server is booted.
     * @param session
     * @return Message Store holding Inbox, Processed Folder an Error Folder.
     */
    private Store connectToMailServer(Session session) {

        Store store;

        try {
            // -- Get hold of a message store, and connect to it --
            store = session.getStore(getProtocol());
            int port = this.loginCredentials.getPort();
            if (port > 0) {
                store.connect(this.loginCredentials.getServerName(), port, this.loginCredentials.getUserName(),
                        this.loginCredentials.getPassword());
            } else {
                store.connect(this.loginCredentials.getServerName(), this.loginCredentials.getUserName(),
                        this.loginCredentials.getPassword());
            }
        } catch (AuthenticationFailedException afe) {
            store = null;
            StringBuffer sbMessage = new StringBuffer("Service: " + this.loginCredentials.getServerName() + "\n");
            sbMessage.append("User: " + this.loginCredentials.getUserName() + "\n\n");
            sbMessage.append("Message: " + afe.getMessage());
            logger.error(Notification.MAIL_SERVICE_LOGIN_FAILED + "\n" + sbMessage.toString());
            SalesforceAgent.processNotification(Notification.MAIL_SERVICE_LOGIN_FAILED, sbMessage.toString(),Notification.SEVERITY_ERROR);
        } catch (MessagingException me) {
            store = null;
            StringBuffer sbMessage = new StringBuffer("Service: " + this.loginCredentials.getServerName() + "\n");
            sbMessage.append("User: " + this.loginCredentials.getUserName() + "\n\n");
            sbMessage.append("Message: " + me.getMessage());
            logger.error(Notification.MAIL_SERVICE_DOWN + "\n" + sbMessage.toString());
            SalesforceAgent.processNotification(Notification.MAIL_SERVICE_DOWN, this.loginCredentials.getServerName() + "\n\n" +me.getMessage(),
                                Notification.SEVERITY_ERROR);
        }

        return store;
    }

    /**
     * Retrieve Messages from the inbox.
     *   First try to retrieve all of them at once, if that fails.
     *   then try one at a time attempting to copy bad messages to the error folder.
     *
     * @param inbox
     * @param readbox
     * @param errorbox
     * @param session
     * @return Array of messages from Inbox
     */
    private Message[] retrieveMessages(Folder inbox, Folder readbox, Folder errorbox, Session session) {

        try {
            return inbox.getMessages();
        } catch (MessagingException me) {
            logger.error(me,me);
            try {
                ArrayList<Message> msgs = new ArrayList<Message>();
                ArrayList<Message> errorMsgs = new ArrayList<Message>();
                Message msg = null;
                for(int i = 1; i <= inbox.getMessageCount(); i++) { //inbox isn't 0 indexed
                    try {
                        msg = inbox.getMessage(i);
                        msgs.add(msg);
                    }  catch (MessagingException me1) {
                        //Delete the bad message from the inbox and move it to the error box
                        if(msg != null) {
                            msg.setFlag(Flags.Flag.DELETED, true);
                            errorMsgs.add(msg);
                        }
                        logger.error(me1,me1);
                    }
                }
                inbox.copyMessages(errorMsgs.toArray(new Message[errorMsgs.size()]), errorbox);
                if(errorMsgs.size() > 0) {
                    String warningText = BAD_MSGS_FOUND.replaceAll("X",String.valueOf(errorMsgs.size()));
                    logger.info(warningText);
                }
                return msgs.toArray(new Message[msgs.size()]);
            }  catch (MessagingException me2) {
                //if inbox.getMessageCount() fails
                handleMessagingError(me2, inbox, readbox, errorbox, session, false);
                return null;
            }
        }
    }

    protected abstract Folder configureInBox(Folder root) throws InvalidConfigurationException, MessagingException;
    protected abstract Folder configureReadBox(Folder root) throws InvalidConfigurationException, MessagingException;
    protected abstract Folder configureErrorBox(Folder root) throws InvalidConfigurationException, MessagingException;

    /**
     * @param folderName The fully qualified, perhaps nested, folder name
     * @param root       The root folder(not inbox)
     * @return           The folder resulting from the resolution of the FQN.
     * @throws InvalidConfigurationException If the root folder is null
     * @throws MessagingException            When other messaging faults occur
     */
    protected Folder getFullyQualifiedFolder(String folderName, Folder root) throws InvalidConfigurationException, MessagingException{

        if (root == null) throw new InvalidConfigurationException(ConfigurationExceptionCode.MAIL_FOLDER_NOT_FOUND);

        char delim = root.getSeparator();
        return root.getFolder(folderName.replace(FOLDER_DELIM ,delim));
    }

    /**
     * Main routine to load emails into salesforce
     *
     * @param msgs
     * @param inbox
     * @param readbox
     * @param errorbox
     * @param session
     * @throws MessagingException
     * @throws IOException
     * @throws FailedBindingException
     * @throws InvalidConfigurationException
     */
    public String[] handleMessage(Message[] msgs, Folder inbox, Folder readbox, Folder errorbox, Session session)
        throws Exception {

        if (msgs.length == 0) return null;

        HandledEmailMessage[] records = new HandledEmailMessage[msgs.length];
        ParsedMessage[] parsedMsgs = new ParsedMessage[msgs.length];
        Message[] messages;
        ArrayList<Message> successMsgs = new ArrayList<Message>();
        ArrayList<Message> errorMsgs = new ArrayList<Message>();
        ArrayList<HandledEmailMessage> recordList = new ArrayList<HandledEmailMessage>();
        ArrayList<Message> goodMsgs = new ArrayList<Message>();
        ArrayList<String> messageIds = new ArrayList<String>();

        /*
         * Convert the JavaMail Messages to ParsedMessages and intercept any errors
         */
        for (int i = 0; i < msgs.length; i++) {
            try {
                ParsedMessage msg = new ParsedMessage(msgs[i]);
                parsedMsgs[i] = msg;
                HandledEmailMessage rec = msg.getEmailMessage();
                recordList.add(rec);
                goodMsgs.add(msgs[i]);
            } catch (Exception me) {
                // If for some reason the message couldn't be parsed, copy it to the error folder and dump
                // a stack trace
                msgs[i].setFlag(Flags.Flag.DELETED, true);
                errorMsgs.add(msgs[i]);
                logger.error(me,me);
            }
        }
        records = recordList.toArray(new HandledEmailMessage[recordList.size()]);
        messages = goodMsgs.toArray(new Message[goodMsgs.size()]);

        try {

            //SOAP API call to handle loading batch of email messages into cases
            PartnerConnection conn = getConnection();
            SaveResult[] results = conn.handleEmailMessage(records);

            for (int i = 0; results != null && i < results.length; i++) {
                SaveResult r = results[i];

                postProcessParsedMessage(parsedMsgs[i], r.isSuccess());

                if (r.isSuccess()) {
                    logger.info(Calendar.getInstance().getTime().toString() + ":" + pSUCCESS +":" + i + " ID=" + r.getId() + " "+ pSUCCESS +"=" + r.isSuccess());
                    messageIds.add(r.getId());
                    successMsgs.add(messages[i]);
                } else {
                    Error error = r.getErrors().length == 0 ? null : r.getErrors()[0];
                    String sMessage = (error == null || error.getMessage() == null) ? "" : error.getMessage();
                    handleAPIError(Calendar.getInstance().getTime().toString() + ":"+ pERROR +":" + i + ": " + sMessage, error, false, false);
                    errorMsgs.add(messages[i]);
                }
            }

            if (readbox != null && this instanceof ImapClient) {
                inbox.copyMessages(successMsgs.toArray(new Message[successMsgs.size()]), readbox);
            }
            if (errorbox != null && this instanceof ImapClient) {
                if(errorMsgs.size() > 0) {
                    logger.info("Copying " + errorMsgs.size() + " messages to error mailbox");
                }
                inbox.copyMessages(errorMsgs.toArray(new Message[errorMsgs.size()]), errorbox);
            }

            //By now the messages are copied to either the Error box or the Processed Box, so mark them for delete.
            for (int i = 0; results != null && i < results.length; i++) {
                messages[i].setFlag(Flags.Flag.DELETED, true);
            }

        } catch (ApiFault e) {
            if (e.getExceptionCode() == ExceptionCode.EMAIL_TO_CASE_NOT_ENABLED) {
                logger.error(EMAIL_TO_CASE_NOT_ENABLED);
                logger.info("Shutting down service next cycle...");
                shutdown(SHUTDOWN_EMAIL_TO_CASE_NOT_ENABLED);
            } else if (e.getExceptionCode() == ExceptionCode.EMAIL_TO_CASE_INVALID_ROUTING) {
                logger.error(EMAIL_TO_CASE_ROUTING_INCORRECT);
                throw new InvalidConfigurationException(ConfigurationExceptionCode.SFDC_ROUTING_ADDRESS_ERROR, e);
            } else if (e.getExceptionCode() == ExceptionCode.INVALID_OPERATION_WITH_EXPIRED_PASSWORD) {
                logger.error(pPASSWORD_EXPIRED);
                throw new InvalidConfigurationException(ConfigurationExceptionCode.SFDC_AUTHENTICATION_ERROR);
            } else {
                inbox.copyMessages(messages, errorbox);
                for(int i = 0; i< messages.length; i++) {
                    messages[i].setFlag(Flags.Flag.DELETED, true);
                }
                handleAPIError(e, true, true);
            }
        } catch (ConnectionException ce) {
            if (ce.getCause() instanceof SocketTimeoutException)
                throw new InvalidConfigurationException(ConfigurationExceptionCode.SFDC_HTTP_TIMEOUT);
            else throw ce;
        } catch (MessagingException e1) {
            handleMessagingError(e1, inbox, readbox, errorbox, session, false);
        } catch (java.lang.OutOfMemoryError e) {
            Runtime runtime = Runtime.getRuntime();
            String sMaxMemory = "-Xmx = " + Long.toString(runtime.maxMemory()) + " bytes";
            throw new InvalidConfigurationException(ConfigurationExceptionCode.AGENT_OUT_OF_MEMORY,new String[] {sMaxMemory});
        } catch (Exception e) {
            //This could be simply a problem with a single message. Let's try to handle each message
            //one by one to find the culprit and move it to the error box
            for(int i = 0; i< records.length; i++) {
                try {
                    conn.handleEmailMessage(new HandledEmailMessage[] {records[i]});
                }
                catch (Exception ex) {
                    inbox.copyMessages(new Message[] {messages[i]}, errorbox);
                    messages[i].setFlag(Flags.Flag.DELETED, true);
                    throw e;
                }
            }
            throw e;
        }

        return messageIds.toArray(new String[messageIds.size()]);
    }

    private void postProcessParsedMessage(ParsedMessage msg, boolean isSuccess) {

        if(isSuccess) {
            HashMap files = msg.getOversizedAttachments();
            if (files != null) {
                Set fileNames = files.keySet();
                String fileName = null;

                Iterator it = fileNames.iterator();
                while(it.hasNext()){
                    try {
                        fileName = (String)it.next();
                        File file = new File(ATTACH_DIR + fileName);
                        boolean directoryCreated = file.getParentFile().mkdirs();
                        if(directoryCreated) {
                            logger.info("Directory created - " + file.getParentFile());
                        }
                        FileChannel channel = new FileOutputStream(file, false).getChannel();
                        channel.write((ByteBuffer) files.get(fileName));
                        channel.close();
                    } catch (SecurityException se) {
                        logger.error("Insufficient access to create external file" + fileName +". ", se);
                    } catch (FileNotFoundException fnf) {
                        logger.error("Error creating file " + fileName +". ", fnf);
                    } catch (IOException e) {
                        logger.error("Error writing to " + fileName +". ", e);
                    }
                }
            }
        }
    }

    private void handleAPIError(Throwable e, boolean sendEmail, boolean forceNewSession) {
        handleAPIError(null, null, e, sendEmail, forceNewSession);
    }

    private void handleAPIError(String msg, Error error, boolean sendEmail, boolean forceNewSession) {
        handleAPIError(msg, error, null, sendEmail, forceNewSession);
    }

    /**
     * This class can be overridden to extract the API errors.
     */
    protected void handleAPIError(String msg, Error error, Throwable t, boolean sendEmail, boolean forceNewSession) {
        // Error is passed in for overriding classes
        String sMsgText;
        if (msg != null) {
            sMsgText = msg;
        } else if (t != null) {
            sMsgText = (t instanceof ApiFault) ? ((ApiFault)t).getExceptionMessage() : t.getMessage();
        } else {
            sMsgText = "No error information was passed in.";
        }

        if (t != null)
            logger.error(sMsgText, t);
        else
            logger.error(sMsgText);

        if (sendEmail)
            SalesforceAgent.processNotification(Notification.SFDC_API_ERROR, sMsgText, Notification.SEVERITY_ERROR);

        if (forceNewSession)
            forceNewSession();
    }

    /**
     * @param e
     * @param inbox
     * @param readbox
     * @param errorbox
     * @param session
     * This was made protected for testing purposes.
     */
    protected void handleMessagingError(MessagingException e, Folder inbox, Folder readbox, Folder errorbox, Session session, boolean shutdown){
        SalesforceAgent.processNotification(e.getMessage(),e.getMessage(),Notification.SEVERITY_ERROR);
        logger.error(e,e);
        try {
            Store store = session.getStore();
            logger.info(pSHUTTING_DOWN_CONNECTION_TO_MAIL_SYSTEM + store.getURLName());

            if (inbox != null && inbox.isOpen()) inbox.close(true);
            if (readbox != null && readbox.isOpen()) readbox.close(false);
            if (errorbox != null && errorbox.isOpen()) errorbox.close(false);

            if (store != null) store.close();

        } catch (Exception ex2) {
            logger.error(ex2, ex2);
        }

        if(shutdown) {
            shutdown(SHUTDOWN_EMAIL_TO_CASE_MESSAGING_ERROR);
        }
    }

    /**
     * @param ice
     * This was made protected for testing purposes.
     */
    protected void handleConfigurationException(InvalidConfigurationException ice){
        logger.error(ice, ice);
        if(ice.isNotifiable()){
            SalesforceAgent.processNotification(ice.getSubject(),ice.getMessage(),Notification.SEVERITY_ERROR);
        }
        if(ice.isFatal()){
            shutdown(SHUTDOWN_EMAIL_TO_CASE_NOT_CONFIGURED);
        }
    }

    /**
     * Determines if the current iteration should process based on the status of this service as well as
     * the number of threads pending in the queue if processing is backed up.  Since each iteration attempts
     * to process all remaining work, there is no real need for a thread to add itself to the queue if there
     * are already MAX_THREADS_PER_SERVICE in front of it.
     *
     * @return true if the process should continue on and process a batch of email.
     */
    private synchronized boolean isOkToContinue() {

        if (isShutdown()) {return false;}

        String sKey = this.loginCredentials.getServerName() + ":" + this.loginCredentials.getUserName();

        if (hashServiceStateTable.containsKey(sKey)) {
            int iThreadCount = hashServiceStateTable.get(sKey);
            if (iThreadCount >= MAX_THREADS_PER_SERVICE) {
                return false;
            } else {
                iThreadCount++;
                hashServiceStateTable.remove(sKey);
                hashServiceStateTable.put(sKey, iThreadCount);
                return true;
            }
        } else {
            hashServiceStateTable.put(sKey, 1);
            return true;
        }
    }

    private synchronized void decrementClientCounter() {

        String sKey = this.loginCredentials.getServerName() + ":" + this.loginCredentials.getUserName();

        if (hashServiceStateTable.containsKey(sKey)) {
            int iThreadCount = hashServiceStateTable.get(sKey);
            iThreadCount--;
            hashServiceStateTable.remove(sKey);
            if (iThreadCount > 0) {
                hashServiceStateTable.put(sKey, iThreadCount);
            }
        }
    }

    public static void printMessage(ParsedMessage message) {
        try {
            logger.debug(message);
            logger.debug("-------------------------------------------------------------------");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    protected static synchronized void shutdown(int _iCode) {
        iShutdownReason = _iCode;
    }

    protected synchronized boolean isShutdown() {
        return iShutdownReason > 0;
    }

    protected synchronized int getShutdownCode() {
        return iShutdownReason;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {

        StringBuffer sbMessage = new StringBuffer("Service: " + this.loginCredentials.getServerName());
        sbMessage.append("\n   User: " + this.loginCredentials.getUserName());
        sbMessage.append("\n   Parent: " + super.toString());
        return sbMessage.toString();
    }
}
