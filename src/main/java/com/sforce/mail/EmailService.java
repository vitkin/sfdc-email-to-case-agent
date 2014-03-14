/*
 * #%L
 * sfdc-email-to-case-agent
 * %%
 * Copyright (C) 2005 salesforce.com, inc.
 * %%
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *    following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 *    the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of salesforce.com, inc. nor the names of its contributors may be used to endorse or
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
 * #L%
 */
package com.sforce.mail;

import java.io.IOException;
import com.sforce.SalesforceAgent;
import com.sforce.SalesforceService;
import com.sforce.SalesforceWorker;
import com.sforce.config.ConfigInfo;
import com.sforce.exception.InvalidConfigurationException;
import com.sforce.exception.InvalidConfigurationException.ConfigurationExceptionCode;
import com.sforce.util.ConsoleReader;

import org.apache.log4j.Logger;
import com.sforce.config.ConfigParameters;

public class EmailService extends SalesforceService {

    private static final String pSCHEDULE_POLL  = "Scheduling poll process against ";
    private static final String pPORT           = "   Port    : ";
    private static final String pUSERID         = "   UserID  : ";
    private static final String pPASSWORD       = "   Password: ";
    private static final String pINTERVAL       = "   Interval: ";
    private static final String pTIMEOUT        = "   TimeOut : ";
    private static final String pINBOX          = "   InBox   : ";
    private static final String pREADBOX        = "   ReadBox : ";
    private static final String pERRORBOX       = "   ErrorBox: ";
    private static final String pMINUTES        = " minute(s)";

    //Logging
    static Logger logger = Logger.getLogger(EmailService.class.getName());

    @Override
    protected void loadService(ConfigInfo config) {
        String[] servers = config.getList();
        if (servers != null) {
            for (int j = 0; j < servers.length; j++) {
                try {
                    String server = servers[j];
                    String url = config.get(server, ConfigParameters.pURL);
                    String port = config.get(server, ConfigParameters.pPORT);
                    String protocol = config.get(server, ConfigParameters.pPROTOCOL);
                    String user = config.get(server, ConfigParameters.pUSERNAME);

                    // Decrypt the password if necessary
                    GenericClient.handlePasswordDecryption(config, server);
                    String pass = config.get(server, ConfigParameters.pPASSWORD);

                    // How often does the agent look for new mail on the mail server
                    String interval = config.get(server, ConfigParameters.pINTERVAL);
                    // For how long does the agent look for new mail on the mail server
                    String timeout = config.get(server, ConfigParameters.pTIMEOUT);
                    // What's the name of the inbox to look for email in
                    String inbox = config.get(server, ConfigParameters.pINBOX);
                    // Where should messages be moved after they are read
                    String readbox = config.get(server, ConfigParameters.pREADBOX);
                    // Where should messages be moved to when there are errors
                    String errorbox = config.get(server, ConfigParameters.pERRORBOX);

                    // Mail Service URL
                    url = promptArgument(url,"Mail Service URL");
                    if(url == null || url.equalsIgnoreCase("")) {
                        throw new InvalidConfigurationException(ConfigurationExceptionCode.MAIL_URL_NOT_FOUND);
                    }

                    // Mail Service Protocol
                    protocol = promptArgument(protocol,"Mail Service Protocol");
                    if (protocol == null || protocol.equalsIgnoreCase("")) {
                        throw new InvalidConfigurationException(ConfigurationExceptionCode.MAIL_PROTOCOL_NOT_FOUND);
                    }
                    if (! protocol.equalsIgnoreCase("pop3") &&
                        ! protocol.equalsIgnoreCase("imap") &&
                        ! protocol.equalsIgnoreCase("imaps"))
                    {
                        throw new InvalidConfigurationException(ConfigurationExceptionCode.MAIL_PROTOCOL_NOT_VALID);
                    }

                    // Mail Service User
                    user = promptArgument(user,"Mail Service User ID");
                    if(user == null || user.equalsIgnoreCase("")) {
                        throw new InvalidConfigurationException(ConfigurationExceptionCode.MAIL_USER_NOT_FOUND);
                    }

                    // Mail Service Password
                    if (null == pass){
                        try {
                            pass = ConsoleReader.readPasswordFromConsole(ConfigParameters.pPASSWORD);
                        } catch (IOException e) {
                            pass = null;
                        }
                    }
                    // Mail Service Polling interval
                    interval = promptArgument(interval,"Polling Interval in Minutes");

                    // Mail Service Polling time out
                    timeout = promptArgument(timeout,"Polling TimeOut in Minutes");

                    // Mail Service Inbox
                    inbox = promptArgument(inbox,"Mailbox for incoming messages");
                    if(inbox == null || inbox.equalsIgnoreCase("")) {
                        throw new InvalidConfigurationException(ConfigurationExceptionCode.MAIL_INBOX_NOT_FOUND);
                    }

                    // Mail Service Readbox
                    readbox = promptArgument(readbox,"Mailbox for holding processed messages");
                    if(inbox == null || inbox.equalsIgnoreCase("")) {
                        throw new InvalidConfigurationException(ConfigurationExceptionCode.MAIL_READBOX_NOT_FOUND);
                    }

                    // Mail Service Errorbox
                    errorbox = promptArgument(errorbox,"Mailbox for holding unprocessed messages");
                    if(errorbox == null || errorbox.equalsIgnoreCase("")) {
                        throw new InvalidConfigurationException(ConfigurationExceptionCode.MAIL_ERRORBOX_NOT_FOUND);
                    }

                    int howOftenMinutes = 15;   // This is just the default if there's no value in the config file.
                    try {
                        howOftenMinutes = Integer.parseInt(interval);
                    } catch (NumberFormatException e) {
                        throw new InvalidConfigurationException(ConfigurationExceptionCode.MAIL_INTERVAL_NOT_VALID,e);
                    }
                    if (howOftenMinutes<1) throw new InvalidConfigurationException(ConfigurationExceptionCode.MAIL_INTERVAL_NOT_VALID);

                    int howLongMinutes = 10;    // This is just the default if there's no value in the config file.
                    try {
                        howLongMinutes = Integer.parseInt(timeout);
                    } catch (NumberFormatException e) {
						            throw new InvalidConfigurationException(ConfigurationExceptionCode.MAIL_TIMEOUT_NOT_VALID, e);
                    }
                    if (howLongMinutes < 1) throw new InvalidConfigurationException(ConfigurationExceptionCode.MAIL_TIMEOUT_NOT_VALID);

                    if (null == inbox || null == readbox || null == errorbox || null == pass || null == user || null == url) {
                        logger.error("Mailbox settings not configured correctly.  Unable to launch server.");
                    } else {
                        LoginCredentials login;
                        if (port == null) {
                            port = "0";
                        }
                        login = new LoginCredentials(url, port, user, pass);

                        GenericClient c = null;
                        if (protocol.equalsIgnoreCase("pop3")) {
                            c = new Pop3Client(login);
                        }
                        if (protocol.equalsIgnoreCase("imap")) {
                            c = new ImapClient(login);
                        }
                        if (protocol.equalsIgnoreCase("imaps")) {
                            c = new ImapSSLClient(login);
                        }
                        if(null == c){
                            throw new InvalidConfigurationException(ConfigurationExceptionCode.MAIL_PROTOCOL_NOT_VALID);
                        }

                        c.setInbox(inbox);
                        c.setReadbox(readbox);
                        c.setErrorbox(errorbox);

                        SalesforceWorker worker = new EmailWorker(c, howLongMinutes * 60 * 1000);

                        logger.info(pSCHEDULE_POLL + url);
                        logger.info(pPORT + (port.equals("0") ? "default" : port) );
                        logger.info(pUSERID + user);
                        logger.info(pPASSWORD);
                        logger.info(pINTERVAL + interval + pMINUTES);
                        logger.info(pTIMEOUT + timeout + pMINUTES);
                        logger.info(pINBOX + inbox);
                        logger.info(pREADBOX + readbox);
                        logger.info(pERRORBOX + errorbox);
                        SalesforceAgent.registerServer(url + ":" + port + ":" + user + ":" + inbox);

                        scheduleAtFixedRate(worker, 0, howOftenMinutes * 60 * 1000);

                    }
                } catch (InvalidConfigurationException ice) {
                    handleConfigurationException(ice);
                }
            }
        }
    }

    private void handleConfigurationException(InvalidConfigurationException ice){
        String sMsg = ice.getMessage();
        logger.error(sMsg);
        if(ice.isNotifiable()){
            SalesforceAgent.processNotification(ice.getSubject(),ice.getMessage(),Notification.SEVERITY_ERROR);
        }
    }

    private String promptArgument(final String sArg, final String sDescription){
        if(null != sArg) return sArg;

        return ConsoleReader.readArgumentFromConsole(sDescription);
    }

    private static class EmailWorker extends SalesforceWorker {
        private final GenericClient client;
        private final int timeout;
        private EmailWorker(GenericClient c, int t) {
            client = c;
            timeout = t;
        }

        @Override
        public void run() {
            if(!client.isShutdown()) {
                final Thread t = new Thread(client);
                t.start();

                try {
                    t.join(timeout);
                } catch (InterruptedException ex) {
                    logger.error(ex, ex);
                }

                if (t.isAlive()) {
                    logger.error("Time out! Interrupting mail client operation...");
                    t.interrupt();
                }
            } else {
                logger.info("Shutting down service...");
                logger.info(client.toString());
                SalesforceAgent.deRegisterServer(client.getUrl() + ":" + client.getPort() + ":" + client.getUser() + ":" + client.getInbox());
                this.cancel();
            }
        }
    }

}
