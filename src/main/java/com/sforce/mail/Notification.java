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

/**
 * Notification
 *
 * Abstract representation of a notificatation to a administrator.
 */
public abstract class Notification {


    public final static String MAIL_SERVICE_DOWN = "Unable to connect to mail service.";
    public final static String MAIL_SERVICE_LOGIN_FAILED = "Unable to connect to mail service, authentication failed.";

    public final static String SFDC_SERVICE_DOWN = "Unable to connect to salesforce.com service.";
    public final static String SFDC_API_ERROR = "Unable to process mail message.";
    public final static String UNKNOWN_ERROR = "Unknown error while processing mail message.";

    public static final String SEVERITY_ERROR = "ERROR";
    public static final String SEVERITY_WARNING = "WARNING";
    public static final String SEVERITY_INFO = "INFO";

    private String severity;
    /** Notification Prefix */
    private String prefix;
    /** Notification Subject and description */
    private String description;
    /** Notification Message */
    private String messageText;
    /** Notification Recipients, comma delimited list of valid email addresses*/
    private String recipients;
    /** Notification Sender */
    private String from;
    /** Credentials used to authenticate the notification request */
    private LoginCredentials credentials;

    public Notification (LoginCredentials _oCredentials){
        severity = "";
        prefix = "";
        description = "";
        messageText = "";
        recipients = "";
        from = "";
        credentials = _oCredentials;
    }

    /**
     * @return Returns the m_oCredentials.
     */
    protected LoginCredentials getCredentials() {
        return credentials;
    }

    /**
     * @param credentials The m_oCredentials to set.
     */
    protected void setCredentials(LoginCredentials credentials) {
        this.credentials = credentials;
    }

    /* (non-Javadoc)
     * @see com.sforce.mail.LoginCredentials#getPassword()
     */
    public String getPassword() {
        return this.credentials.getPassword();
    }

    /* (non-Javadoc)
     * @see com.sforce.mail.LoginCredentials#getServerName()
     */
    public String getServerName() {
        return this.credentials.getServerName();
    }

    /* (non-Javadoc)
     * @see com.sforce.mail.LoginCredentials#getUserName()
     */
    public String getUserName() {
        return this.credentials.getUserName();
    }

    /**
     * @return Returns the m_sPrefix.
     */
    public String getPrefix() {
        return this.prefix;
    }

    /**
     * @param prefix The m_sPrefix to set.
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * @return Returns the m_sDescription.
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * @param description The m_sDescription to set.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return Returns the m_sFrom.
     */
    public String getFrom() {
        return this.from;
    }

    /**
     * @param from The m_sFrom to set.
     */
    public void setFrom(String from) {
        this.from = from;
    }

    /**
     * @return Returns the m_sMessageText.
     */
    public String getMessageText() {
        return this.messageText;
    }

    /**
     * @param messageText The m_sMessageText to set.
     */
    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    /**
     * @return Returns the m_sSeverity.
     */
    public String getSeverity() {
        return this.severity;
    }

    /**
     * @param severity The m_sSeverity to set.
     */
    public void setSeverity(String severity) {
        this.severity = severity;
    }

    /**
     * @return Returns the m_sTo.
     */
    public String getTo() {
        return this.recipients;
    }

    /**
     * @param to The m_sTo to set.
     */
    public void setTo(String to) {
        this.recipients = to;
    }

    public abstract void sendNotification();

}
