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
package com.sforce.exception;


public class InvalidConfigurationException extends Exception {

    private static final long serialVersionUID = 1L;

    public static enum ConfigurationExceptionCode {
        MAIL_ERRORBOX_NOT_VALID("The error folder provided is not valid.", true, false),
        MAIL_ERRORBOX_NOT_FOUND("No configuration value has been provided for the error folder.", true, false),
        MAIL_INBOX_NOT_VALID("The Inbox provided is not valid.", true, false),
        MAIL_INBOX_NOT_FOUND("No configuration value has been provided for the Inbox.", true, false),
        MAIL_READBOX_NOT_VALID("The processed folder provided is not valid.", true, false),
        MAIL_READBOX_NOT_FOUND("No configuration value has been provided for the processed folder.", true, false),
        MAIL_URL_NOT_FOUND("No configuration value has been provided for the mail service url.", true, false),
        MAIL_PROTOCOL_NOT_FOUND("No configuration value has been provided for the mail service protocol.", true, false),
        MAIL_PROTOCOL_NOT_VALID("The mail protocol provided is not valid.", true, false),
        MAIL_USER_NOT_FOUND("No configuration value has been provided for the mail service User ID.", true, false),
        MAIL_AUTHENTICATION_ERROR("Invalid mail service username or password in configuration.", true, false),
        MAIL_FOLDER_NOT_FOUND("A mail folder provided is not valid.", true, false),
        MAIL_INTERVAL_NOT_VALID("The mail polling interval specified in the conguration file is not valid. It must be an integer greater than or equal to 1.", true, true),

        SFDC_CFG_FILE_LOAD_FAILURE("Failed to load salesforce.com configuration file.", true, true),
        SFDC_URL_NOT_FOUND("No configuration value was found for the salesforce.com service url.", false, false),
        SFDC_URL_NOT_VALID("The url provided for the salesforce.com service is not valid.", false, false),
        SFDC_HTTP_TIMEOUT("Connection timed out prior to receipt of response, increase timeout increment.", true, false),
        SFDC_AUTHENTICATION_ERROR("Invalid or locked out salesforce.com user ID or password in configuration.", true, true),
        SFDC_MISSING_USERNAME("No configuration value was found provided for the salesforce.com User Id.", true, true),
        SFDC_MISSING_PASSWORD("No configuration value was found for the salesforce.com Password.", true, true),
        SFDC_MISSING_LOGIN_URL("No configuration value was found for the salesforce.com Login URL.", true, true),
        SFDC_MISSING_LARGE_ATTACHMENT_DIR("No configuration value was found for the large attachment directory.", true, true),
        SFDC_INVALID_LARGE_ATTACHMENT_DIR("Invalid directory specified for large attachments.", true, true),
        SFDC_MISSING_LARGE_ATTACHMENT_URL("No configuration value was found for the large attachment URL.", true, true),
        SFDC_MISSING_LARGE_ATTACHMENT_SIZE("No configuration value was found for the large attachment size.", true, true),
        SFDC_INVALID_LARGE_ATTACHMENT_SIZE("Invalid maximum size specified for large attachments. It must be an number (size in MB) greater than or equal to 0.", true, true),
        SFDC_ROUTING_ADDRESS_ERROR("Routing addresses are not setup correctly.", true, true),

        AGENT_SERVICE_NOT_FOUND("No configuration value has been provided for the Email Agent Service Locator.", false, false),
        AGENT_SERVICE_NOT_VALID("The Email Agent Service Locator is invalid.", false, false),
        AGENT_SERVICE_CONFIG_NOT_FOUND("The Email Agent Service configuration file could not be found.", false, false),
        AGENT_OUT_OF_MEMORY("The Email Agent Service is configured with too low of a JVM memory heap.", true, true),

        NOTIFICATION_HOST_NOT_FOUND("No configuration value has been provided for the notification host.", false, false),
        NOTIFICATION_CLASS_NOT_FOUND("No configuration value has been provided for the notification service class.", false, false),
        NOTIFICATION_CLASS_NOT_VALID("The notification service class is invalid.", false, false),
        NOTIFICATION_SENDER_NOT_FOUND("No configuration value has been provided for the notification service sender.", false, false),
        NOTIFICATION_TARGET_NOT_FOUND("No configuration value has been provided for the notification service receiver.", false, false),
        NOTIFICATION_USER_NOT_FOUND("No configuration value has been provided for the notification service userID.", false, false),
        NOTIFICATION_PASSWORD_NOT_FOUND("No configuration value has been provided for the notification service password.", false, false),

        UNKNOWN_CONFIG_ERROR("Configuration Error.", false, false);

        private String message;
        private boolean notifiable;
        private boolean fatal;

        private ConfigurationExceptionCode(String message, boolean notifiable, boolean fatal) {
            this.message = message;
            this.notifiable = notifiable;
            this.fatal = fatal;
        }

        public String getMessage() {
            return message;
        }

        public boolean isFatal() {
            return fatal;
        }

        public boolean isNotifiable() {
            return notifiable;
        }
    }

    private final ConfigurationExceptionCode exceptionCode;
    private final String[] asContext;

    public InvalidConfigurationException(ConfigurationExceptionCode exceptionCode) {
        this(exceptionCode, (String[]) null);
    }

    public InvalidConfigurationException(ConfigurationExceptionCode exceptionCode, String[] asContext) {
        super(exceptionCode.getMessage());
        this.exceptionCode = exceptionCode;
        this.asContext = asContext;
    }

    public InvalidConfigurationException(ConfigurationExceptionCode exceptionCode, Throwable cause) {
        super(exceptionCode.getMessage(), cause);
        this.exceptionCode = exceptionCode;
        asContext = null;
    }


    public ConfigurationExceptionCode getExceptionCode(){
        return exceptionCode;
    }

    @Override
    public String getMessage() {
        StringBuffer sbMsg = new StringBuffer(this.getExceptionCode().getMessage());
        if(null != asContext){
            for(int i = 0; i < asContext.length; i++) {
                sbMsg.append("\n   " + asContext[i]);
            }
        }
        return sbMsg.toString();
    }

    public String getSubject() {
        return "Configuration Problem. Error Code - " + exceptionCode;
    }

    public boolean isNotifiable() {
        return this.getExceptionCode().isNotifiable();
    }

    public boolean isFatal() {
        return this.getExceptionCode().isFatal();
    }

}
