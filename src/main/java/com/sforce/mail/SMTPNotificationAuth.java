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

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

import org.apache.log4j.Logger;

/**
 * SMTPNotificationAuth
 *
 * Implementation of a SMTPNotification with authentication.
 *
 * Sends an email message if a connection can be made to the SMTP server using the
 * login credentials.
 *
 */
public class SMTPNotificationAuth extends SMTPNotification {

    static Logger logger = Logger.getLogger(SMTPNotificationAuth.class.getName());

    public SMTPNotificationAuth(LoginCredentials _oCredentials) {
        super(_oCredentials);
    }

    @Override
    protected Authenticator getAuthenticator() {
        return new SMTPAuthenticator(getCredentials());
    }

    @Override
    protected void addProperties(Properties p) {
         p.put("mail.smtp.auth", "true");
    }

    private static class SMTPAuthenticator extends javax.mail.Authenticator {

        private final LoginCredentials loginCredentials;
        public SMTPAuthenticator(LoginCredentials loginCredentials) {
            super();
            logger.info("Generating SMTP Authentication Request");
            this.loginCredentials = loginCredentials;
        }

        @Override
        protected  PasswordAuthentication getPasswordAuthentication() {
            PasswordAuthentication auth = new PasswordAuthentication(this.loginCredentials.getUserName(), this.loginCredentials.getPassword());
            return auth;
        }
    }
}
