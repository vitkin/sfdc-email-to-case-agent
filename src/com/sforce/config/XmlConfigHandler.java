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
package com.sforce.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class XmlConfigHandler extends DefaultHandler {
    private ConfigInfo config = new ConfigInfo();
    private StringBuffer textNode = new StringBuffer();

    private String currentArea = null;
    private String currentName = null;

    private XmlConfigHandler() {

    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        if ("configFile".equals(localName)) return;

        if (this.currentArea == null) {
            this.currentArea = localName;
        } else if (this.currentName == null) {
            this.currentName = localName;
        } else {
            throw new RuntimeException("Unrecognized format near: " + localName);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if ("configFile".equals(localName)) return;

        if (this.currentName != null && this.currentArea != null) {
            this.config.put(this.currentArea, this.currentName, textNode.toString().trim());
            this.textNode = new StringBuffer();
            this.currentName = null;
            return;
        }

        if (this.currentName == null) {
            this.currentArea = null;
            return;
        }

        if (this.currentArea == null) {
            throw new RuntimeException("Unrecognized format near: " + localName);
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        textNode.append(ch, start,length);
    }


    public static ConfigInfo parse(String payLoad) throws IOException {
        XmlConfigHandler handler = null;
        try {
            // handle special microsoft characters here.
            // Do not use public void characters(...) to do character stripping.
            char[] ch = payLoad.toCharArray();
            for (int i = 0; i < ch.length; i++) {
                if (ch[i] < 0x20 && !(ch[i] == 0x09 || ch[i] == 0x0a || ch[i] == 0x0d)) {
                    ch[i] = '?';
                }
            }
            payLoad = String.valueOf(ch);

            handler = new XmlConfigHandler();

            // Initialize the XML parser.
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);

            // Create a JAXP SAXParser
            SAXParser saxParser = spf.newSAXParser();

            try {
                saxParser.parse(new ByteArrayInputStream(payLoad.getBytes("UTF-8")), handler);
            } catch (SAXException se) {
                throw new RuntimeException("XML Parse Error: " + se + "\n\n" + payLoad);
            }
        } catch(Exception e) {
            throw new RuntimeException("Error parsing XML: " + e);
        }
        return handler.config;
    }

}
