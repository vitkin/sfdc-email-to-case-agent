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
 * 1) Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *    following disclaimer.
 * 
 * 2) Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 *    the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * 3) Neither the name of salesforce.com, inc. nor the names of its contributors may be used to endorse or
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
package com.sforce;

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.sforce.config.ConfigInfo;
import com.sforce.config.ConfigParameters;
import com.sforce.config.XmlConfigHandler;
import com.sforce.mail.GenericClient;
import com.sforce.mail.LoginCredentials;
import com.sforce.mail.Notification;
import com.sforce.util.ConsoleReader;
import com.sforce.exception.InvalidConfigurationException;
import com.sforce.exception.InvalidConfigurationException.ConfigurationExceptionCode;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class SalesforceAgent {
    private static final String pJAVA_SYS_PROPS = "             J A V A    S Y S T E M   P R O P E R T I E S";
    // The SALESFORCE_AGENT_VERSION must be in Major.Minor version(1.x, not 1.x.1) so that it appears correctly in the Login History
    public static final String SALESFORCE_AGENT_VERSION = "1.9";
    public static final String SALESFORCE_AGENT_VERSION_MSG = "Email To Case Agent v" + SALESFORCE_AGENT_VERSION;
    private static final double[] SUPPORTED_SALESFORCE_API_VERSIONS = { 29.0 };

    public static ConfigInfo GLOBAL_CONFIG;
    public static Map<String, SalesforceService> timers = new HashMap<String, SalesforceService>();
    public static Map<String, Object> nonTimers = new HashMap<String, Object>();
    private static Set<String> setServers = Collections.synchronizedSet(new HashSet<String>(3));

    // Strings
    private static final String pSTART_UP_MESSAGE      = "Starting EmailToCase Agent v" + SALESFORCE_AGENT_VERSION;
    private static final String pSHUTDOWN_MESSAGE      = "EmailToCase Agent Shut Down.";
    private static final String pLOADING_CFG_FILE      = "Loading configuration file ";
    private static final String pFATAL_ERROR           = "FATAL EXCEPTION";
    private static final String pPARSING_CFG_FILE      = "Parsing config file: ";
    private static final String pCFG_PARSED_OK         = "Config successfully parsed";
    private static final String pFILE_NOT_FOUND        = "File Not Found: ";
    private static final String pSTART_SERVICE_MESSAGE = "Attempting to start service ";
    private static final String pWITH_CFG_FILE         = " with configuration file ";
    private static final String pLOADED_AS_TIMER       = " loaded as a timer service.";
    private static final String pLOADED_AS_NONTIMER    = " loaded as a non-timer service.";
    private static final String pSYSTEM_NOTIFY_TARGET  = "System notifications will be sent to ";

    public static final String SERVICES                = "services";

    //Logging
    static Logger logger = Logger.getLogger(SalesforceAgent.class.getName());


    public static void main(String[] args) {

        try {
            System.out.println("\n" + SALESFORCE_AGENT_VERSION_MSG + "\n");

            // The shutdown hook here is simply so that we can output a message
            // to the console to indicate that the Agent has been shutdown, either
            // voluntarily or not...
            Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHook()));

            if (args.length < 2) {
                showUsage();
                System.exit(0);
            }
            String fileName = null;
            fileName = args[0];

            String sLog4JCfg = args[1];
            PropertyConfigurator.configure(sLog4JCfg);
            logger.info(pSTART_UP_MESSAGE);

            logJavaProperties();

            GLOBAL_CONFIG = loadConfig(fileName);
            logger.info(pLOADING_CFG_FILE + fileName);

            if (GLOBAL_CONFIG.get(ConfigParameters.pADMIN, ConfigParameters.pNOTIFY_ON_ERROR) != null) {
                logger.info(pSYSTEM_NOTIFY_TARGET + GLOBAL_CONFIG.get(ConfigParameters.pADMIN, ConfigParameters.pNOTIFY_ON_ERROR));
            }

            try {
                checkEssentials(GLOBAL_CONFIG);
            }
            catch (InvalidConfigurationException e) {
                handleConfigurationException(e);
            }

            String[] services = GLOBAL_CONFIG.getList(SERVICES);
            if (services != null) {
                for (int i = 0; i < services.length; i++) {
                    String serviceName = services[i];
                    String serviceConfigFile = GLOBAL_CONFIG.get(SERVICES, services[i]);
                    ConfigInfo serviceConfig = loadConfig(serviceConfigFile);
                    if (serviceConfig == null) {
                        logger.info("Unable to create configuration for service: " + serviceName + " with file " + serviceConfigFile);

                    } else {
                        logger.info(pSTART_SERVICE_MESSAGE + serviceName + pWITH_CFG_FILE + serviceConfigFile);

                        Class serviceClass = Class.forName(serviceName);
                        Constructor constructor = serviceClass.getConstructor(new Class[0]);
                        Object serviceObj = constructor.newInstance(new Object[0]);

                        if (serviceObj instanceof SalesforceService) {
                            SalesforceService service = (SalesforceService)serviceObj;
                            timers.put(serviceName, service);
                            service.loadService(serviceConfig);
                            logger.info(serviceName + " " + pLOADED_AS_TIMER);
                        } else {
                            nonTimers.put(serviceName, serviceObj);
                            logger.info(serviceName + " " + pLOADED_AS_NONTIMER);
                        }
                    }
                }
            }
            while (true) {
                Thread.sleep(10000);
                if(! haveActiveServices()) {
                   System.exit(0);
                }
            }
        } catch (Exception ex) {
            logger.fatal(pFATAL_ERROR,ex);
        }
        logger.info(pSHUTDOWN_MESSAGE);
        //System.exit(0);
    }

    /**
     * Verifies that the required config file exists and that the required fields
     * in the config file are present and valid, and throws an InvalidConfigurationException otherwise.
     *
     * @param config - The config file that contains the salesforce.com credential information
     * @throws InvalidConfigurationException if there is an error in the config file.
     */
    private static void checkEssentials(ConfigInfo config) throws InvalidConfigurationException {
        if (config == null) {
            throw new InvalidConfigurationException(ConfigurationExceptionCode.SFDC_CFG_FILE_LOAD_FAILURE);
        }

        /*
         * For user name, try to accept the input from the console if it has not been
         * provided...
         */
        if (config.get(ConfigParameters.pSFDC_LOGIN, ConfigParameters.pUSERNAME) == null) {

            String sUserName = ConsoleReader.readArgumentFromConsole("Salesforce.com UserID");
            if (sUserName != null) {
                config.put(ConfigParameters.pSFDC_LOGIN, ConfigParameters.pUSERNAME, sUserName);
            } else {
                throw new InvalidConfigurationException(ConfigurationExceptionCode.SFDC_MISSING_USERNAME);
            }
        }

        /*
         * For password, try to accept the input from the console if it has not been
         * provided and make sure to prevent the password from being shadowed to the console
         */
        if (config.get(ConfigParameters.pSFDC_LOGIN, ConfigParameters.pPASSWORD) == null && 
        	config.get(ConfigParameters.pSFDC_LOGIN, ConfigParameters.pENCRYPTED_PASSWORD) == null) {
            try {
                String sPassword = ConsoleReader.readPasswordFromConsole("Salesforce.com Password");
                if (sPassword != null) {
                    config.put(ConfigParameters.pSFDC_LOGIN, ConfigParameters.pPASSWORD, sPassword);
                } else {
                    throw new InvalidConfigurationException(ConfigurationExceptionCode.SFDC_MISSING_PASSWORD);
                }
            } catch (IOException e) {
                throw new InvalidConfigurationException(ConfigurationExceptionCode.SFDC_MISSING_PASSWORD, e);
            }
        }

        /*
         * Check what version of the API we are requesting and make sure it's a supported version.  Print
         * an warning otherwise.
         */
        String loginUrl = config.get(ConfigParameters.pLOGIN, ConfigParameters.pURL);
        if (loginUrl != null) {
    		boolean apiVersionIsSupported = false;
        	try {
        		String[] loginParts = loginUrl.split("/");
        		double version = Double.valueOf(loginParts[loginParts.length-1]);

        		for(double supportedVersion: SUPPORTED_SALESFORCE_API_VERSIONS) {
        			if(supportedVersion == version) {
        				apiVersionIsSupported = true;
        				break;
        			}
        		}
        	} catch (NumberFormatException e) {
        		logger.warn("Encountered problem while determining configured API version from loginUrl: "+loginUrl, e);
        	}

        	if(!apiVersionIsSupported) {
        		logger.warn("Detected a potentially unsupported API version.  Please consider updating your salesforce.com login endpoint to use API version 29.0.  By default, this configuration is specified in the sfdcConfig.xml file under 'url' in the 'sfdcLogin' section.  We recommend setting it to <url>https://login.salesforce.com/services/Soap/u/29.0</url>.");
        	}
        } else {
        	throw new InvalidConfigurationException(ConfigurationExceptionCode.SFDC_MISSING_LOGIN_URL);
        }

        /*
         * For attachments, if the attachments area is present, then make sure that
         * largeAttachmentDirectory, largeAttachmentURLPrefix and largeAttachmentSize are
         * present and valid as well.
         */
        if (config.contains(ConfigParameters.pATTACH)) {
            String sizeStr = config.get(ConfigParameters.pATTACH, ConfigParameters.pATTACH_SIZE);
            if (sizeStr==null)
                throw new InvalidConfigurationException(ConfigurationExceptionCode.SFDC_MISSING_LARGE_ATTACHMENT_SIZE);
            else {
                double size = -1;
                try {
                    size = Double.parseDouble(sizeStr);
                }
                catch (NumberFormatException e) {
                    throw new InvalidConfigurationException(ConfigurationExceptionCode.SFDC_INVALID_LARGE_ATTACHMENT_SIZE, e);
                }
                if (size<0) throw new InvalidConfigurationException(ConfigurationExceptionCode.SFDC_INVALID_LARGE_ATTACHMENT_SIZE);
            }

            String dir = config.get(ConfigParameters.pATTACH, ConfigParameters.pATTACH_DIR);
            if (dir==null)
                throw new InvalidConfigurationException(ConfigurationExceptionCode.SFDC_MISSING_LARGE_ATTACHMENT_DIR);
            else {
                File f = new File(dir);
                boolean isValid = f.exists();
                if (!isValid) isValid = f.mkdirs();
                if (!isValid) throw new InvalidConfigurationException(ConfigurationExceptionCode.SFDC_INVALID_LARGE_ATTACHMENT_DIR);
                else {
                    //try creating a file in the folder to make sure we have the correct access rights
                    try {
                        File testFile = File.createTempFile("testAccess","test",f);
                        testFile.delete();
                    }
                    catch (IOException io) {
                        throw new InvalidConfigurationException(ConfigurationExceptionCode.SFDC_INVALID_LARGE_ATTACHMENT_DIR, io);
                    }
                }
            }

            String url = config.get(ConfigParameters.pATTACH, ConfigParameters.pATTACH_URL);
            if (url==null)
                throw new InvalidConfigurationException(ConfigurationExceptionCode.SFDC_MISSING_LARGE_ATTACHMENT_URL);

        }
    }

    /**
     * loadConfig loads an xml configuration file from the file indicated by fileName
     *
     * @param fileName    The name of the file that contains the configuration information.
     *
     * @return ConfigInfo A map representation of the xml file.
     *
     * @throws IOException
     */
    private static ConfigInfo loadConfig(String fileName) throws IOException {
        ConfigInfo config = null;

        if (fileName != null && fileName.trim().length() > 0) {
            try {
                logger.info(pPARSING_CFG_FILE + fileName);
                File configFile = new File(fileName);
                String content = readFile(configFile);
                config = XmlConfigHandler.parse(content);
                logger.info(pCFG_PARSED_OK);
                config.logConfig(fileName);
            } catch (FileNotFoundException ex) {
                logger.error(pFILE_NOT_FOUND + fileName + " : " + ex.getMessage(),ex);
            }
        }
        return config;
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

    /**
     * Display Agent argument usage to console
     *
     */
    private static void showUsage() {

        System.out.println("\nEmail2Case usage:\n");
        System.out.println("    java -jar Email2Case.jar sfdcConfig.xml log4j.properties\n");

    }

    /**
     * @return boolean indicating active services are being polled so the agent
     *                  should not be shut down.
     */
    private static boolean haveActiveServices(){
        Iterator iter;
        boolean bReturn = false;

        synchronized(setServers) {
            iter = setServers.iterator();
            StringBuffer sbMsg = new StringBuffer("Services still in operation:");
            while(iter.hasNext()) {
                sbMsg.append("\n   " + (String) iter.next());
            }
            if(setServers.size() > 0) {
                bReturn = true;
            }
        }
        return bReturn;

    }

    /**
     * logs the error, notify by email and shuts down if necessary
     * @param ice
     */
    private static void handleConfigurationException(InvalidConfigurationException ice){
        logger.error(ice, ice);
        if(ice.isNotifiable()){
            SalesforceAgent.processNotification(ice.getSubject(),ice.getMessage(),Notification.SEVERITY_ERROR);
        }
        if(ice.isFatal()){
            System.exit(0);
        }
    }

    /**
     * Processes Notifications (SMTP or other extensions) for any of the classes in
     * these packages.
     *
     * @param sDescription
     * @param sText
     * @param sSeverity
     * @param asContext
     */
    public static void processNotification(String sDescription, String sText, String sSeverity, String[] asContext) {

        try {
            String host = SalesforceAgent.GLOBAL_CONFIG.get(ConfigParameters.pNOTIFY, ConfigParameters.pHOST);
            String port = SalesforceAgent.GLOBAL_CONFIG.get(ConfigParameters.pNOTIFY, ConfigParameters.pPORT);
            String user = SalesforceAgent.GLOBAL_CONFIG.get(ConfigParameters.pNOTIFY, ConfigParameters.pUSER);
            String password = SalesforceAgent.GLOBAL_CONFIG.get(ConfigParameters.pNOTIFY, ConfigParameters.pPASSWORD);
            String classname = SalesforceAgent.GLOBAL_CONFIG.get(ConfigParameters.pNOTIFY, ConfigParameters.pSERVICE);

            if(host == null) {
                throw new InvalidConfigurationException(ConfigurationExceptionCode.NOTIFICATION_HOST_NOT_FOUND);
            }

            if(user == null) {
                throw new InvalidConfigurationException(ConfigurationExceptionCode.NOTIFICATION_USER_NOT_FOUND);
            }

            LoginCredentials lc = new LoginCredentials(host, port, user, password);


            StringBuffer sbMsgText = new StringBuffer(sText + "\n");
            String[] asThisContext = asContext;
            if (asThisContext == null) {
                asThisContext = getAgentContext();
            }

            for(int i=0; i< asThisContext.length; i++) {
                sbMsgText.append("\n" + asThisContext[i]);
            }


            Class classNotification = Class.forName(classname);
            Constructor constructor = classNotification.getConstructor(new Class[] {Class.forName("com.sforce.mail.LoginCredentials")});
            Object objectNotification = constructor.newInstance(new Object[] { lc });
            if (objectNotification instanceof Notification) {
                Notification notification = (Notification)objectNotification;
                notification.setDescription(sDescription);
                notification.setMessageText(sbMsgText.toString());
                notification.setSeverity(sSeverity);
                notification.setFrom(SalesforceAgent.GLOBAL_CONFIG.get(ConfigParameters.pNOTIFY, ConfigParameters.pFROM));
                notification.setTo(SalesforceAgent.GLOBAL_CONFIG.get(ConfigParameters.pNOTIFY, ConfigParameters.pNOTIFY_EMAIL));
                notification.sendNotification();
            } else {
                throw new InvalidConfigurationException(ConfigurationExceptionCode.NOTIFICATION_CLASS_NOT_VALID, new String[] {classname});
            }
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
    }

    private static String [] getAgentContext() {
        StringBuffer sbMsg = new StringBuffer();
        sbMsg.append("\nSFDC Url:           " + SalesforceAgent.GLOBAL_CONFIG.get(ConfigParameters.pSFDC_LOGIN, ConfigParameters.pURL));
        sbMsg.append("\nSFDC User ID:       " + SalesforceAgent.GLOBAL_CONFIG.get(ConfigParameters.pSFDC_LOGIN, ConfigParameters.pUSERNAME));

        String sRefresh = SalesforceAgent.GLOBAL_CONFIG.get(ConfigParameters.pSFDC_LOGIN, ConfigParameters.pLOGIN_REFRESH);
        if (sRefresh == null) {
            sRefresh = String.valueOf(GenericClient.defaultRefresh);
        }
        sbMsg.append("\nSFDC Login Refresh: " + sRefresh);

        String sTimeout = SalesforceAgent.GLOBAL_CONFIG.get(ConfigParameters.pSFDC_LOGIN, ConfigParameters.pTIMEOUT);
        if (sTimeout == null) {
            sTimeout = String.valueOf(GenericClient.defaultTimeout);
        }
        sbMsg.append("\nSFDC Timeout:       " + sTimeout);

        /*
         * List all registered Timers polling mail servers...
         */
        sbMsg.append("\n\nRegistered Services:");

        synchronized(setServers) {
            Iterator iter = setServers.iterator();
            while(iter.hasNext()) {
                sbMsg.append("\n   " + (String) iter.next());
            }
        }



        return new String[] {sbMsg.toString()};
    }

    /**
     * Processes Notifications (SMTP or other extensions) for any of the classes in
     * these packages.
     *
     * @param sDescription
     * @param sText
     * @param sSeverity
     */
    public static void processNotification(String sDescription, String sText, String sSeverity) {
        processNotification(sDescription, sText, sSeverity, null);
    }

    /**
     * Registers a Server that is being polled for incoming mail.
     * In order to shut down the entire agent all servers must be de-registered
     * first.
     *
     * @param _sServerID
     * @return status
     */
    public static boolean registerServer(String _sServerID) {
        synchronized(setServers) {
            if(setServers.contains(_sServerID)) {
                logger.error("Service " + _sServerID + " already registered");
                return false;
            }

            logger.info("Service " + _sServerID + " successfully registered");
            setServers.add(_sServerID);
            return true;
        }
    }

    /**
     * De-registers a server from the server list.  This list is used to determine
     * if the agent can be safely shutdown.
     *
     * @param _sServerID
     */
    public static void deRegisterServer(String _sServerID) {
        synchronized(setServers) {
            if(setServers.contains(_sServerID)) {
                logger.info("Service " + _sServerID + " successfully de-registered");
                setServers.remove(_sServerID);
            } else {
                logger.error("Service " + _sServerID + " could not be de-registered, not found");
            }
        }
    }

    private static void logJavaProperties() {
        logger.info("============================================================================");
        logger.info(pJAVA_SYS_PROPS);
        logger.info("============================================================================");
        java.util.Properties p = System.getProperties();
        java.util.Enumeration keys = p.keys();
        while( keys.hasMoreElements() ) {
            String s = (String)keys.nextElement();
            logger.info(s + "= " + System.getProperty(s));
        }
        logger.info("============================================================================");
    }

    private static class ShutdownHook implements Runnable{

        public void run() {
            System.out.println("\nSalesforce.com Email to Case Agent Shutting down.\n\n");
        }
    }

}
