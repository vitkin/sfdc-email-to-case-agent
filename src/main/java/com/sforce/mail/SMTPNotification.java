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

import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;

import com.sforce.util.TextUtil;

/**
 * SMTPNotification
 *
 * Implementation of a SMTPNotification w/o authentication.
 *
 * Sends an email message to a list of recipients specified in the recipients member
 * variable delimited by commas
 *
 */
public class SMTPNotification extends Notification {

    static Logger logger = Logger.getLogger(SMTPNotification.class.getName());

    /**
     *
     */
    public SMTPNotification(LoginCredentials _oCredentials) {
        super(_oCredentials);
    }

    /*
     *
     * @see com.sforce.mail.Notification#sendNotification()
     */
    @Override
    public void sendNotification() {
        // Create new message
        try {
            MimeMessage msg = new MimeMessage(getMailSession());
            msg.setDescription(this.getDescription());
            msg.setSubject(this.getDescription());
            msg.setText(this.getMessageText());

            Address addrFrom = new InternetAddress(this.getFrom());
            msg.setFrom(addrFrom);

            List recipients = TextUtil.splitSimple(",", this.getTo());
            Iterator it = recipients.iterator();
            while(it.hasNext()) {
                Address addrTo = new InternetAddress((String)it.next());
                msg.addRecipient(Message.RecipientType.TO, addrTo);
            }

            Transport.send(msg);

        } catch (Exception e) {
            logErrors(e);
        }
    }

    private Session getMailSession() {

        // Throw exception if mail host or user are not set
        if (this.getServerName() == null || this.getUserName() == null) {
            logErrors(null);
        }

        Properties p = new Properties();
        p.put("mail.host", this.getServerName());
        p.put("mail.user", this.getUserName());

        if (getCredentials().getPort() > 0) {
            p.put("mail.smtp.port", String.valueOf(getCredentials().getPort()));
        }

        // Can define and initialize other session
        // properties here, if desired
        addProperties(p);

        Session oMailSession = Session.getInstance(p, getAuthenticator());
        return oMailSession;
    }

    protected Authenticator getAuthenticator() {
        return null;
    }

    protected void addProperties(Properties p) {}

    private void logErrors(Throwable e) {
        logger.error("---SMTPNotification Error Report ---");
        logger.error("ServerName: " + this.getServerName());
        logger.error("ServerPort: " + getCredentials().getPort());
        logger.error("UserName: " + this.getUserName());
        logger.error("Subclass: " + this.getClass());
        logger.error("From: " + this.getFrom());
        logger.error("To: " + this.getTo());
        logger.error("Severity: " + this.getSeverity());
        logger.error("Description: " + this.getDescription());
        logger.error("Message: " + this.getMessageText());
        if (null != e) {
            logger.error(e,e);
        }
    }
}
