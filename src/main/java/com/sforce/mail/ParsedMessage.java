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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimePart;
import javax.mail.internet.MimeUtility;

import org.apache.log4j.Logger;

import com.sforce.SalesforceAgent;
import com.sforce.config.ConfigParameters;
import com.sforce.soap.partner.wsc.EmailAttachment;
import com.sforce.soap.partner.wsc.HandledEmailMessage;
import com.sforce.soap.partner.wsc.NameValuePair;
import com.sforce.util.TextUtil;
import com.sun.mail.imap.IMAPMessage;


/**
 * Representation of a parsed email message
 */
public class ParsedMessage {
    private static final String pCONTENT_TYPE_WARNING = "Content Type Lookup Failed.  Attempting to retrieve content type from header data.";
    private static final String charactersToReplace = "\"*:/\\?<>|";
    private static final String replacementCharacters = "_________";
    private static final DateFormat unsafeFormat = new SimpleDateFormat("yyyyMMdd_HHmmssSSS");
    private static final String PROXY_TEXT = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\"><html><head><title>@@FILENAME@@</title></head><body><a href=\"@@FILENAME@@\">@@FILENAME@@</a></body></html>";
    private static final String PROXY_EXT = ".html";

    private static final String DEFAULT_CONTENT_TYPE = "TEXT/PLAIN;";

    private EmailHeaders headers;
    private EmailFrom from;
    private EmailSubject subject;
    private EmailBody body;

    private static final String ATTACHMENT_PROCESSING_DEFAULT = "DEFAULT";

    private static final Map<String, String> contentTypeLookup;
    private static final int MAX_FILE_NAME = 80;
    private static final Pattern IS_ALPHA;

    static {
        contentTypeLookup = new HashMap<String, String>(16);

        contentTypeLookup.put("multipart/mixed", ATTACHMENT_PROCESSING_DEFAULT);
        contentTypeLookup.put("multipart/alternative", ATTACHMENT_PROCESSING_DEFAULT);
        contentTypeLookup.put("multipart/related", ATTACHMENT_PROCESSING_DEFAULT);
        contentTypeLookup.put("multipart/digest", ATTACHMENT_PROCESSING_DEFAULT);
        contentTypeLookup.put("multipart/report", ATTACHMENT_PROCESSING_DEFAULT);
        contentTypeLookup.put("multipart/parallel", ATTACHMENT_PROCESSING_DEFAULT);
        contentTypeLookup.put("multipart/appledouble", ATTACHMENT_PROCESSING_DEFAULT);
        contentTypeLookup.put("multipart/header-set", ATTACHMENT_PROCESSING_DEFAULT);
        contentTypeLookup.put("multipart/voice-message", ATTACHMENT_PROCESSING_DEFAULT);
        contentTypeLookup.put("multipart/form-data", ATTACHMENT_PROCESSING_DEFAULT);
        contentTypeLookup.put("multipart/x-mixed-replace", ATTACHMENT_PROCESSING_DEFAULT);
        contentTypeLookup.put("message/rfc822", ATTACHMENT_PROCESSING_DEFAULT);
        contentTypeLookup.put("message/partial", ATTACHMENT_PROCESSING_DEFAULT);
        contentTypeLookup.put("message/external-body", ATTACHMENT_PROCESSING_DEFAULT);
        contentTypeLookup.put("message/news", ATTACHMENT_PROCESSING_DEFAULT);
        contentTypeLookup.put("message/http", ATTACHMENT_PROCESSING_DEFAULT);

        IS_ALPHA = Pattern.compile("[\\p{L}[\\p{N}]]");
    }

    static Logger logger = Logger.getLogger(ParsedMessage.class.getName());

    private static final String pCONTENT_TYPE = "Content-Type";

    private static String getAttachUrl() {
        return SalesforceAgent.GLOBAL_CONFIG.get(ConfigParameters.pATTACH, ConfigParameters.pATTACH_URL);
    }

    private static double getAttachMaxSize() {
        String size = SalesforceAgent.GLOBAL_CONFIG.get(ConfigParameters.pATTACH, ConfigParameters.pATTACH_SIZE);
        double dSize = Double.MAX_VALUE;
        if(size == null) return dSize;

        try {
            dSize = Double.parseDouble(size) * 1024 * 1024;
        } catch (NumberFormatException nfe) {
            dSize = Double.MAX_VALUE;
        }
        return dSize;
    }

    /**
     * Attempt to retrieve content type from the part, if the part is unable to comply
     * retrieve it manually from the headers ourselves.  This is a more robust implementation
     * to deal with quirky mailserver implementations and Javamail issues.
     *
     * @param part
     * @return contentType
     * @throws MessagingException
     * @throws UnsupportedEncodingException
     */
    static String getPartContentType(Part part) throws MessagingException, UnsupportedEncodingException {
        String contentType = null;
        try {
            contentType = part.getContentType();
        } catch (MessagingException me) {
            logger.warn(pCONTENT_TYPE_WARNING);
            String[] contentTypes = part.getHeader(pCONTENT_TYPE);
            contentType = decodeHeader(contentTypes);
        }
        return contentType;
    }



    public ParsedMessage(Message message) throws MessagingException, IOException {
        this.headers = new EmailHeaders(message);
        this.from = new EmailFrom(message);
        this.subject = new EmailSubject(message);
        this.body = new EmailBody(message);
    }

    public EmailFrom getFrom() {
        return this.from;
    }

    public HandledEmailMessage getEmailMessage() {
        HandledEmailMessage email = new HandledEmailMessage();
        email.setHeaders(this.headers.getEmailMessageHeaders());
        email.setSubject(this.subject.subject);
        email.setTextBody(this.body.textMessage);
        email.setHtmlBody(this.body.htmlMessage);

        EmailBody.EmailAttachment[] attachments = this.body.getEmailAttachments();
        EmailAttachment[] attachRecords = new EmailAttachment[attachments.length];
        for (int i = 0; i < attachments.length; i++) {
            attachRecords[i] = new EmailAttachment();
            attachRecords[i].setFileName(attachments[i].name);
            byte[] decoded = attachments[i].content;
            attachRecords[i].setBody(decoded);
            attachRecords[i].setContentType(attachments[i].getContentType());
        }
        email.setAttachments(attachRecords);
        return email;
    }

    /**
     * @return HashMap <String, ByteBuffer>
     */
    public HashMap<String,ByteBuffer> getOversizedAttachments() {
        HashMap<String,ByteBuffer> map = new HashMap<String, ByteBuffer>();
        EmailBody.EmailAttachment[] attachments = this.body.getOversizedAttachments();
        if (attachments == null || attachments.length == 0) return map;

        for (int i = 0; i < attachments.length; i++) {
            map.put(attachments[i].getFilename(), ByteBuffer.wrap(attachments[i].content));
        }
        return map;
    }

    @Override
    public String toString() {
        StringBuffer buff = new StringBuffer();
        buff.append("\n-- HEADERS: --\n" + this.headers);
        buff.append("\n-- FROM: --\n" + this.from);
        buff.append("\n-- SUBJECT: --\n" + this.subject);
        buff.append("\n-- BODY: --\n" + this.body);
        return buff.toString();
    }


    private static String cleanString(String value) {

        if (value == null) return value;

        StringBuffer buff = new StringBuffer();

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            switch (c) {
            case '\n': // Line Feed is OK
            case '\r': // Carriage Return is OK
            case '\t': // Tab is OK
                // These characters are specifically OK, as exceptions to the general rule below:
                buff.append(c);
                break;
            default:
                if (((c >= 0x20) && (c <= 0xD7FF)) || ((c >= 0xE000) && (c <= 0xFFFD))) {
                    buff.append(c);
                }
            // For chars outside these ranges (such as control chars),
            // do nothing; it's not legal XML to print these chars,
            // even escaped
            }
        }
        return buff.toString();
    }


    /**
     *
     */
    private static class EmailHeaders {
        private final EmailHeader[] headers;
        private EmailHeaders(Message message) throws MessagingException {
            ArrayList<EmailHeader> headerList = new ArrayList<EmailHeader>();

            Enumeration enumHeaders = message.getAllHeaders();
            while(enumHeaders.hasMoreElements()) {
                Header header = (Header)enumHeaders.nextElement();
                String value = header.getValue();
                String name = header.getName();
                EmailHeader h = new EmailHeader(name, value);
                headerList.add(h);
            }
            this.headers = headerList.toArray(new EmailHeader[headerList.size()]);
        }

        public NameValuePair[] getEmailMessageHeaders() {
            NameValuePair[] headerRecords = new NameValuePair[this.headers.length];
            for (int i = 0; i < this.headers.length; i++) {
                headerRecords[i] = new NameValuePair();
                headerRecords[i].setName(cleanString(this.headers[i].headerName));
                String decoded = this.headers[i].headerValue;
                try {
                    decoded = cleanString(MimeUtility.decodeText(decoded));
                } catch (UnsupportedEncodingException e) {
                }
                headerRecords[i].setValue(decoded);

            }
            return headerRecords;
        }


        @Override
        public String toString() {
            StringBuffer buff = new StringBuffer();
            for (int i = 0; i < this.headers.length; i++) {
                buff.append(this.headers[i]);
                if (i < (this.headers.length-1)) buff.append("\n");
            }
            return buff.toString();
        }

        private static class EmailHeader {
            private final String headerName;
            private final String headerValue;
            private EmailHeader(String name, String value) {
                this.headerName = name;
                this.headerValue = value;
            }
            @Override
            public String toString() {
                return "  " + this.headerName + " = " + this.headerValue;
            }
        }
    }

    private static String decodeHeader(String[] headerValue) throws UnsupportedEncodingException {

        if (headerValue == null) {
            throw new UnsupportedEncodingException();
        }
        try {

            StringBuffer val = new StringBuffer();
            for (int i = 0; i < headerValue.length; i++) {
                if (null != headerValue[i]) {
                    val.append(MimeUtility.decodeText(headerValue[i]));
                }
            }
            return val.toString();
        } catch (Exception e) {
            throw new UnsupportedEncodingException();
        }
    }

    /**
     *
     */
    private static class EmailFrom {
        private static final String pFROM_ADDRESS_WARNING = "Failed To Load From Address.  Attempt to retrieve From Address from headers.";
        private static final String pFROM = "From";
        private final EmailAddress[] from;

        /**
         * Attempt to retrieve the From address from the message but if this fails
         * fall through and retrieve it from the headers.  We're doing this in to
         * make the agent more robust in response to quirky mail servers and Javamail issues.
         *
         * @param message
         * @return From internetAddress
         * @throws MessagingException
         * @throws UnsupportedEncodingException
         */
        private static InternetAddress[] getHeaderFrom(Message message) throws MessagingException, UnsupportedEncodingException {
            InternetAddress[] addresses = null;
            try {
                addresses = (InternetAddress[])message.getFrom();
            } catch (MessagingException e) {
                logger.warn(pFROM_ADDRESS_WARNING);

                String[] froms = message.getHeader(pFROM);
                if (froms != null) {
                    addresses = new InternetAddress[froms.length];
                    for (int i = 0; i < froms.length; i++) {
                        addresses[i] = new InternetAddress(MimeUtility.decodeText(froms[i]), false);
                    }
                }
            }
            return addresses;
        }

        private EmailFrom(Message message) throws MessagingException, UnsupportedEncodingException {
            InternetAddress[] fromAddresses = getHeaderFrom(message);

            if (fromAddresses != null) {
                ArrayList<EmailAddress> fromList = new ArrayList<EmailAddress>(fromAddresses.length);
                for (int i = 0; i < fromAddresses.length; i++) {
                    InternetAddress fromAddress = fromAddresses[i];
                    EmailAddress emailFrom = new EmailAddress(cleanString(fromAddress.getPersonal()), cleanString(fromAddress.getAddress()));
                    fromList.add(emailFrom);
                }
                this.from = fromList.toArray(new EmailAddress[fromList.size()]);
            } else {
                this.from = new EmailAddress[0];
            }
        }

        @Override
        public String toString() {
            StringBuffer buff = new StringBuffer();
            for (int i = 0; i < this.from.length; i++) {
                buff.append(this.from[i].toString());
                if (i < (this.from.length-1)) buff.append("\n");
            }
            return buff.toString();
        }

        /**
         *
         */
        private static class EmailAddress {
            private final String fromName;
            private final String fromAddress;
            private EmailAddress(String name, String address) {
                this.fromName = cleanString(name);
                this.fromAddress = cleanString(address);
            }
            @Override
            public String toString() {
                return this.fromName + " [" + this.fromAddress + "]";
            }
        }
    }


    /**
     *
     */
    private static class EmailSubject {
        private String subject;
        private EmailSubject(Message message) throws MessagingException {
            try {
                this.subject = decodeHeader(message.getHeader("Subject"));
            } catch (UnsupportedEncodingException e) {
                this.subject = message.getSubject();
            }
        }
        @Override
        public String toString() {
            return subject;
        }
    }


    /**
     *
     */
    protected static class EmailBody {
        private static final String CONTENT_TYPE_TEXT = "text/plain";
        private static final String CONTENT_TYPE_HTML = "text/html";
        private static final String CONTENT_TYPE_MULTIPART = "multipart";

        private String textMessage;
        private String htmlMessage;
        private ArrayList<EmailAttachment> attachments;
        private ArrayList<EmailAttachment> largeAttachments;

        private EmailBody(Message message) throws MessagingException, IOException {
            this.attachments = new ArrayList<EmailAttachment>();
            this.largeAttachments = new ArrayList<EmailAttachment>();

            try {
                Part[] parts = flattenMessageParts(message);
                StringBuffer text = new StringBuffer();
                StringBuffer html = new StringBuffer();
                for (int i = 0; i < parts.length; i++) {
                    Part part = parts[i];
                    String deposition = part.getDisposition();
                    if (deposition != null && deposition.equalsIgnoreCase(Part.ATTACHMENT)) {
                        addAttachment(part);
                    } else {
                        String type = ParsedMessage.getPartContentType(part);

                        if (type != null) {
                            type = type.toLowerCase();
                            if (type.startsWith(CONTENT_TYPE_TEXT)) {
                                Object content = part.getContent();
                                text.append(content);
                                text.append("\n\n");
                            } else if (type.startsWith(CONTENT_TYPE_HTML)) {
                                Object content = part.getContent();
                                html.append(content);
                                html.append("\n\n");
                            } else {
                                // Add it as a attachment if we do not recognize the contentType
                                addAttachment(part);
                            }
                        }
                    }
                }

                this.textMessage = cleanString(text.toString().trim());
                this.htmlMessage = cleanString(html.toString().trim());
            } catch (MessagingException me) {
                this.textMessage = getRawContent(message);
                this.htmlMessage = "";
            } catch (IOException ioe) {
                this.textMessage = getRawContent(message);
                this.htmlMessage = "";
            }
        }

        protected void addAttachment(Part part) throws MessagingException, IOException {
            EmailAttachment attach = new EmailAttachment(part);
            double maxSize = getAttachMaxSize();
            if (attach.size() >= maxSize && maxSize >= 0) {
                logger.info("Removing Large Attachment from Email");
                String filename = makeAttachmentFilename(attach);
                attach.setFilename(filename);
                this.largeAttachments.add(attach);
                //Create a small attachment that Acts as a Proxy to the Large attachment being stored externally.
                EmailAttachment proxy = new EmailAttachment(filename + PROXY_EXT, getAttachUrl() + filename,
                        CONTENT_TYPE_HTML);
                this.attachments.add(proxy);
            } else if (attach.size() == 0) {
                logger.warn("Attachment cannot be empty, skipping file: " + attach.getFilename());
            } else {
                this.attachments.add(attach);
            }
        }

        private String makeAttachmentFilename(EmailAttachment att) {
            long dateMS = System.currentTimeMillis();
            String filename = TextUtil.removeWhitespace(att.getFilename());
            filename = TextUtil.translate(filename, charactersToReplace, replacementCharacters);

            String dateTime;
            synchronized(unsafeFormat) {
                dateTime = unsafeFormat.format(new Date(dateMS));
            }
            filename = dateTime.substring(0,8) + File.separator + dateTime + "_" + filename;
            return  filename.replace('\\', '/');
        }

        /**
         * Retrieve the raw input from an IMAPMessage as a MIME multi-part part.
         *
         * @param part
         * @return String representing the text version of the message
         * @throws MessagingException
         * @throws IOException
         */
        private String getRawContent(Part part) throws MessagingException, IOException {

            StringBuffer buffer = new StringBuffer(4096);

            if (part instanceof IMAPMessage) {

                IMAPMessage imapMessage = (IMAPMessage)part;
                InputStream instream = imapMessage.getRawInputStream();
                BufferedInputStream sbstream = new BufferedInputStream(instream);
                byte[] b = new byte[512];
                int read = sbstream.read(b);
                buffer.append(new String(b));

                while(read > 0) {
                    read = sbstream.read(b);
                    if (read > 0) {
                        buffer.append(new String(b));
                    }
                }
            }
            return cleanString(buffer.toString());
        }

        @Override
        public String toString() {
            StringBuffer buff = new StringBuffer();
            if (this.textMessage.length() > 0) {
                buff.append("TEXT:");
                buff.append("\n");
                buff.append(this.textMessage);
                buff.append("\n");
            }
            if (this.htmlMessage.length() > 0) {
                buff.append("HTML:");
                buff.append("\n");
                buff.append(this.htmlMessage);
                buff.append("\n");
            }

            if (this.attachments.size() > 0) {
                buff.append("ATTACHMENTS:");
                for (int i = 0; i < this.attachments.size(); i++) {
                    buff.append("\n");
                    buff.append(this.attachments.get(i));
                }
            }
            return buff.toString();
        }

        private Part[] flattenMessageParts(Part part) throws MessagingException, IOException {
            ArrayList<Part> flattened = new ArrayList<Part>();
            flattenMessageParts(part, flattened);
            return flattened.toArray(new Part[flattened.size()]);
        }

        /**
         * Helper method to flatten the nested parts of a multiPart message.
         */
        private void flattenMessageParts(Part part, ArrayList<Part> parts) throws MessagingException, IOException {
            String contentType = ParsedMessage.getPartContentType(part);

            if (contentType != null && contentType.startsWith(CONTENT_TYPE_MULTIPART)) {
                Object mPart = part;
                if (!(mPart instanceof Multipart)) {
                    mPart = part.getContent();
                }
                if (mPart instanceof Multipart) {
                    Multipart multiPart = (Multipart)mPart;
                    int size = multiPart.getCount();
                    for (int i = 0; i < size; i++) {
                        flattenMessageParts(multiPart.getBodyPart(i), parts);
                    }
                }
            } else {
                parts.add(part);
            }
        }

        private EmailAttachment[] getEmailAttachments() {
            //Return only this.attachments, not this.largeAttachments.
            return this.attachments.toArray(new EmailAttachment[this.attachments.size()]);
        }

        private EmailAttachment[] getOversizedAttachments() {
            //Return only this.attachments, not this.largeAttachments.
            return this.largeAttachments.toArray(new EmailAttachment[this.largeAttachments.size()]);
        }

        protected static class EmailAttachment {
            private final String contentType;
            private String name;
            private final byte[] content;
            public int size() { return content.length; }

            protected static String getFileName(Part part) throws MessagingException {
                String fname = null;
                //first try and get the data from filename
                try {
                    fname = part.getFileName();
                } catch (MessagingException ex){
                }
                //decode if it needs it
                if (fname != null && fname.startsWith("=?")){
                    try{
                        fname = MimeUtility.decodeWord( fname );

                    }
                    catch( Exception ex){
                    }
                }

                //if you cant get it from filename, try description
                if (fname == null || fname.length() < 1) {
                    fname = part.getDescription();
                }

                //if not there use content id
                if ((fname == null || fname.length() < 1) && part instanceof MimePart ) {
                    fname = ((MimePart) part).getContentID();
                }

                //if all else fails - punt, just hardcode it
                if (fname == null || fname.length() < 1) {
                    fname = "-";
                }

                //truncate to 80
                if (fname.length() > MAX_FILE_NAME) {
                    int idx = fname.lastIndexOf('.');
                    if (idx < 0 || (fname.length() - idx) >= MAX_FILE_NAME) {
                        fname = fname.substring(0, MAX_FILE_NAME);
                    } else {
                        String name = fname.substring(0, MAX_FILE_NAME - (fname.length() - idx));
                        fname = name.concat(fname.substring(idx, fname.length())); //preserve extension
                    }
                }

                return fname;
            }

            /**
             * If the attachment is an embedded Message it does not have a filename and it will have
             * a known content type that can be processed.
             *
             * @param part
             * @return true if this is an Embedded Email Message, false otherwise.
             * @throws MessagingException
             */
            private boolean isEmbeddedMessage(Part part) throws MessagingException {

                if (contentTypeLookup.containsKey(this.contentType.toLowerCase())) {
                    if(part.getFileName() == null) {
                        return true;
                    }
                }
                return false;
            }

            private int findFirstAlphaNumeric(String sIn) {

                Matcher m = IS_ALPHA.matcher(sIn);
                if(m.find()){
                    try {
                        int start = m.start();
                        return start;
                    } catch ( IllegalStateException ise) {
                        return -1;
                    }
                } else {
                    return -1;
                }
            }

            private String cleanFilename(String nameIn) {

                String nameOut;

                if (nameIn.length() > (MAX_FILE_NAME - 5)) { // include room for ".txt" or ".html"
                    nameOut = nameIn.substring(0, MAX_FILE_NAME - 6);
                } else {
                    nameOut = nameIn;
                }

                int iFirst  = findFirstAlphaNumeric(nameOut);

                // In the case that the subject just isn't going to provide a valid filename
                // then generate a generic one...
                if(iFirst < 0 || iFirst > nameOut.length()) {
                    return "MSG_" + System.currentTimeMillis();
                }

                if(iFirst != 0) {
                    nameOut = nameOut.substring(iFirst);
                }

                nameOut = TextUtil.removeWhitespace(nameOut);
                nameOut = TextUtil.translate(nameOut, charactersToReplace, replacementCharacters);

                return nameOut;
            }

            /**
             *
             * @param part
             * @return array of bytes representing the embedded content in a part.
             * @throws MessagingException
             * @throws IOException
             */
            private byte[] getEmbeddedContent(Part part) throws MessagingException, IOException {

                try {
                    ParsedMessage msg = new ParsedMessage(new MimeMessage(null,part.getInputStream()));
                    StringBuffer sbMsg = new StringBuffer(1024);
                    String sFileExtension;

                    if (msg.body.htmlMessage == null || msg.body.htmlMessage.length() <= 0) {
                        sbMsg.append(msg.body.textMessage + "\n\n");
                        sFileExtension = ".txt";
                    } else {
                        sbMsg.append(msg.body.htmlMessage);
                        sFileExtension = ".html";
                    }


                    this.name = cleanFilename(msg.subject.subject);
                    this.name = this.name + sFileExtension;
                    return sbMsg.toString().getBytes();
                } catch (MessagingException me) {
                    logger.error(me, me);
                    return new byte[0];
                }
            }

            private EmailAttachment(String filename, String url) {
                this(filename,url,DEFAULT_CONTENT_TYPE);
            }

            private EmailAttachment(String filename, String url, String contentType) {

                this.contentType = contentType;
                this.name = filename;
                String html = TextUtil.replaceSimple(PROXY_TEXT, new String[] {"@@FILENAME@@"}, new String[] {url});
                this.content = html.getBytes();
            }

            private EmailAttachment(Part part) throws MessagingException, IOException {

                this.contentType = ParsedMessage.getPartContentType(part);
                byte[] attachmentContent = null;
                try {
                    if (isEmbeddedMessage(part)) {
                        attachmentContent = getEmbeddedContent(part);
                    }
                    else {

                        this.name = getFileName(part);
                        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
                        byte[] buffer = new byte[255];

                        try {
                            InputStream stream = part.getInputStream();
                            int numRead = stream.read(buffer);
                            while (numRead != -1) {
                                bStream.write(buffer, 0, numRead);
                                numRead = stream.read(buffer);
                            }
                        } catch (IOException ioe) {
                            logger.error(ioe,ioe);
                        }

                        attachmentContent = bStream.toByteArray();
                    }
                } catch (MessagingException me) {
                    logger.error(me, me);
                    attachmentContent = new byte[0];
                }

                this.content = attachmentContent;
            }

            public String getFilename() {
                return this.name;
            }

            public void setFilename(String name) {
                this.name = name;
            }

            @Override
            public String toString() {
                return this.name;
            }

            public String getContentType() {
                return this.contentType;
            }
        }
    }

}

