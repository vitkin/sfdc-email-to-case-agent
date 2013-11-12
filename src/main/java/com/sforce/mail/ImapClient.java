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

import javax.mail.Folder;
import javax.mail.MessagingException;

import com.sforce.exception.InvalidConfigurationException;
import com.sforce.exception.InvalidConfigurationException.ConfigurationExceptionCode;


/**
 * Imap client implementation
 */
public class ImapClient extends GenericClient {
    public ImapClient(LoginCredentials loginCredentials) {
        super(loginCredentials);
    }

    /**
     * This constructor was added for testing purposes.
     *
     */
    protected ImapClient(){

    }

    @Override
    protected String getProtocol() {
        return "imap";
    }

    @Override
    protected Folder configureInBox(Folder root) throws InvalidConfigurationException, MessagingException {
        Folder inbox = configureMailbox(getInbox(), root, ConfigurationExceptionCode.MAIL_INBOX_NOT_VALID);
        return inbox;
    }

    @Override
    protected Folder configureReadBox(Folder root) throws InvalidConfigurationException, MessagingException {
        return configureMailbox(getReadBoxName(), root, ConfigurationExceptionCode.MAIL_READBOX_NOT_VALID);
    }

    @Override
    protected Folder configureErrorBox(Folder root) throws InvalidConfigurationException, MessagingException {
        return configureMailbox(getErrorBoxName(), root, ConfigurationExceptionCode.MAIL_ERRORBOX_NOT_VALID);
    }

    private Folder configureMailbox(String folderName, Folder root, ConfigurationExceptionCode exceptionCode) throws InvalidConfigurationException, MessagingException {

        Folder mailbox = getFullyQualifiedFolder(folderName, root);

        if (mailbox == null || ! mailbox.exists()) {
            throw new InvalidConfigurationException(exceptionCode, new String[]{getProtocol(), folderName});
        }

        return mailbox;
    }
}

