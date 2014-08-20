/*
 * #%L
 * sfdc-email-to-case-agent
 * %%
 * Copyright (C) 2006 salesforce.com, inc.
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
package com.sforce.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

import javax.mail.AuthenticationFailedException;
import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;

import com.sforce.config.ConfigInfo;
import com.sforce.config.ConfigParameters;
import com.sforce.config.XmlConfigHandler;
import com.sforce.exception.InvalidConfigurationException;
import com.sforce.exception.InvalidConfigurationException.ConfigurationExceptionCode;
import com.sforce.mail.LoginCredentials;
import com.sforce.mail.Notification;

/**
 *  MailUtil usage:<br><br>
 *
 *     java MailUtil.class email2case.xml service command [command2 ...n]<br><br>
 *
 *      email2case.xml ==&gt; the configuration file for your mail servers.<br>
 *      service ==&gt; the particular mail server to report on as identified in the config file.<br><br>
 *
 *      Commands:<br>
 *        Folders   : List all folders available on the mail server.<br>
 *        MsgCount  : Show count of messages in the Inbox, Error and Processed folders.<br>
 *
 */
public class MailUtil {

    private static final String PROTOCOL = "imap";

    private static HashMap<CommandType, String> commands = new HashMap<CommandType, String>();
    private static String configFilename;
    private static String service;
    public static ConfigInfo CONFIG;
    private static LoginCredentials LOGIN_CREDENTIALS;
    private static Store store = null;
    private static Folder inbox = null;
    private static Folder error = null;
    private static Folder processed = null;

    private static enum CommandType {
        LIST_FOLDERS, MSG_COUNT, SEND_MSG;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            processArguments(args);
            loadConfig();
            connectToServer();
            processCommands();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            System.exit(1);
        } finally {
            try {
                closeServer();
            } catch (MessagingException e) {
            }
        }
    }

    private static void processArguments(String[] args) {
        if(args.length == 1 && args[0].endsWith("?")) {
            showUsage();
            System.exit(1);
        }

        if(args.length < 3) {
            System.out.println("Error: too few arguments");
            showUsage();
            System.exit(1);
        }

        configFilename = args[0];
        service = args[1];
        for(int i=2; i < args.length; i++) {
            if (args[i].toUpperCase().indexOf("FOLD") > -1) {
                commands.put(CommandType.LIST_FOLDERS, "");
            }
            if (args[i].toUpperCase().indexOf("COUNT") > -1) {
                commands.put(CommandType.MSG_COUNT, "");
            }
            if (args[i].toUpperCase().indexOf("SEND") > -1 && (i+1) < args.length) {
                commands.put(CommandType.SEND_MSG, args[++i]);
            }
        }

    }

    private static void showUsage() {

        System.out.println("\nMailServerInfo usage:\n");
        System.out.println("   java MailServerInfo.class email2case.xml service command [command2 ...n]\n\n");
        System.out.println("      email2case.xml ==> the configuration file for your mail servers.\n");
        System.out.println("      service ==> the particular mail server to report on as identified in the config file.\n");
        System.out.println("      commands:\n");
        System.out.println("        Folders   : List all folders available on the mail server.\n");
        System.out.println("        MsgCount  : Show count of messages in the Inbox, Error and Processed folders.\n");
    }

    private static void processCommands() throws MessagingException {
        Iterator<CommandType> it = commands.keySet().iterator();
        while(it.hasNext()) {
            switch (it.next()) {
            case LIST_FOLDERS:
                listFolders();
                break;
            case MSG_COUNT:
                showMsgCounts();
                break;
            case SEND_MSG:
                sendMessages(commands.get(CommandType.SEND_MSG));
                break;
            default:
                break;
            }
        }

    }

    private static void connectToServer() throws MessagingException, InvalidConfigurationException {
        try {
            // -- Get hold of the default session --
            Properties props = System.getProperties();
            Session session = Session.getDefaultInstance(props, null);
            // -- Get hold of a message store, and connect to it --
            store = session.getStore(PROTOCOL);
            int port = LOGIN_CREDENTIALS.getPort();
            if (port > 0) {
                store.connect(LOGIN_CREDENTIALS.getServerName(), port, LOGIN_CREDENTIALS.getUserName(),
                        LOGIN_CREDENTIALS.getPassword());
            } else {
                store.connect(LOGIN_CREDENTIALS.getServerName(), LOGIN_CREDENTIALS.getUserName(),
                        LOGIN_CREDENTIALS.getPassword());
            }
        } catch (AuthenticationFailedException afe) {
            StringBuffer sbMessage = new StringBuffer("Service: " + LOGIN_CREDENTIALS.getServerName() + "\n");
            sbMessage.append("User: " + LOGIN_CREDENTIALS.getUserName() + "\n\n");
            sbMessage.append("Message: " + afe.getMessage());
            System.out.println(Notification.MAIL_SERVICE_LOGIN_FAILED + "\n" + sbMessage.toString());
        } catch (MessagingException me) {
            StringBuffer sbMessage = new StringBuffer("Service: " + LOGIN_CREDENTIALS.getServerName() + "\n");
            sbMessage.append("User: " + LOGIN_CREDENTIALS.getUserName() + "\n\n");
            sbMessage.append("Message: " + me.getMessage());
            System.out.println(Notification.MAIL_SERVICE_DOWN + "\n" + sbMessage.toString());
        }
        Folder folder = store.getDefaultFolder();
        if (folder == null) {
            System.out.println("Error: No Default Folder");
            closeServer();
            System.exit(1);
        }

        inbox = getFullyQualifiedFolder(CONFIG.get(service, ConfigParameters.pINBOX), folder);
        error = getFullyQualifiedFolder(CONFIG.get(service, ConfigParameters.pERRORBOX), folder);
        processed = getFullyQualifiedFolder(CONFIG.get(service, ConfigParameters.pREADBOX), folder);

    }

    /**
     * @param folderName The fully qualified, perhaps nested, folder name
     * @param root       The root folder(not inbox)
     * @return           The folder resulting from the resolution of the FQN.
     * @throws InvalidConfigurationException If the root folder is null
     * @throws MessagingException            When other messaging faults occur
     */
    protected static Folder getFullyQualifiedFolder(String folderName, Folder root) throws InvalidConfigurationException, MessagingException{

        if (root == null) throw new InvalidConfigurationException(ConfigurationExceptionCode.MAIL_FOLDER_NOT_FOUND);

        char delim = root.getSeparator();
        return root.getFolder(folderName.replace('.' ,delim));
    }

    private static void closeServer() throws MessagingException {
        if (inbox != null && inbox.isOpen()) inbox.close(true);
        if (processed != null && processed.isOpen()) processed.close(false);
        if (error != null && error.isOpen()) error.close(false);
        if (store != null) store.close();
    }
    /**
     * loadConfig loads an xml configuration file from the file indicated by fileName
     *
     * @throws IOException
     */
    private static void loadConfig() throws IOException {
        ConfigInfo config = null;

        if (configFilename != null && configFilename.trim().length() > 0) {
            try {
                File configFile = new File(configFilename);
                String content = readFile(configFile);
                config = XmlConfigHandler.parse(content);
            } catch (FileNotFoundException ex) {
                System.out.println("Error: File not found " + configFilename + ex.getMessage());
            }
        }

        CONFIG = config;
        String host = CONFIG.get(service, ConfigParameters.pURL);
        String user = CONFIG.get(service, ConfigParameters.pUSERNAME);
        String password = CONFIG.get(service, ConfigParameters.pPASSWORD);

        if(host == null) {
            System.out.println("Error: Mail Host not found");
            System.exit(1);
        }

        if(user == null) {
            System.out.println("Error: Mail User not found");
            System.exit(1);
        }

        LOGIN_CREDENTIALS = new LoginCredentials(host, user, password);
    }

    /**
     * Helper function to read the contents of a file and return a string represntation.
     *
     * @param file input file
     *
     * @throws IOException
     */
    private static String readFile(File file) throws IOException {
        FileReader fileReader = new FileReader(file);

        StringBuffer content = new StringBuffer();

        int read = fileReader.read();
        while (read != -1) {
            content.append((char)read);
            read = fileReader.read();
        }
        return content.toString();
    }

    private static void listFolders() throws MessagingException {
        Folder folder = store.getDefaultFolder();
        if (folder == null) {
            System.out.println("Error: No Default Folder");
            System.exit(1);
        }

        Folder[] allFolders = folder.list();
        System.out.println("====================  Folder List  ====================");
        for(int i=0; i<allFolders.length; i++){
            System.out.println(allFolders[i].getFullName());
        }
    }

    private static void showMsgCounts() throws MessagingException {
        System.out.println("====================  Message Counts  ====================");
        System.out.println(inbox.getMessageCount() + " messages in " + inbox.getFullName());
        System.out.println(processed.getMessageCount() + " messages in " + processed.getFullName());
        System.out.println(error.getMessageCount() + " messages in " + error.getFullName());
    }

    private static void sendMessages(String configFile) {

    }

}

