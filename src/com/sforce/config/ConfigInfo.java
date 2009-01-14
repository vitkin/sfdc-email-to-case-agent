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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import org.apache.log4j.Logger;

public class ConfigInfo {
    private Map<String, Map<String,String>> config = new HashMap<String, Map<String,String>>();

    private static final String pCONFIG_PROPS = "            C O N F I G U R A T I O N   P R O P E R T I E S";
    private static final String pFILE_NOT_FOUND = "File Not Found: ";
    private static final String pCONTENT = "Content:\n";
    private static final String pPARSING_CONFIG_FILE = "Parsing config file: ";

    // Logging
    static Logger logger = Logger.getLogger(ConfigInfo.class.getName());

    public void loadConfig(String fileName) throws IOException {
        ConfigInfo localconfig = null;

        if (fileName != null && fileName.trim().length() > 0) {
            try {
                logger.info(pPARSING_CONFIG_FILE + fileName);
                File configFile = new File(fileName);
                String content = readFile(configFile);
                logger.info(pCONTENT + content);
                localconfig = XmlConfigHandler.parse(content);
                config = localconfig.config;
            } catch (FileNotFoundException ex) {
                logger.error(pFILE_NOT_FOUND + fileName,ex);
            }
        }
    }

    /*
     * readFile is a helper function to read the contents of a file and return
     * a string representation.
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

    public void put(String area, String name, String value) {
        Map<String, String> areaConfig = this.config.get(area);
        if (areaConfig == null) {
            areaConfig = new HashMap<String,String>();
            this.config.put(area, areaConfig);
        }
        areaConfig.put(name, value);
    }

    public String get(String area, String name) {
        Map<String, String> areaConfig = this.config.get(area);
        if (areaConfig == null) return null;
        return areaConfig.get(name);
    }

    public String[] getList() {
        Set<String> keys = this.config.keySet();
        return keys.toArray(new String[keys.size()]);
    }
    public String[] getList(String area) {
        Map<String, String> areaConfig = this.config.get(area);
        if (areaConfig == null) return null;

        Set<String> keys = areaConfig.keySet();
        return keys.toArray(new String[keys.size()]);
    }

    public boolean contains(String area) {
        return this.config.containsKey(area);
    }

    public void logConfig(String fileName) {
        logger.info("============================================================================");
        logger.info(pCONFIG_PROPS);
        logger.info("============================================================================");
        logger.info("file: " + fileName);

        String[] areas = getList();
        for(int i=0; i<areas.length; i++) {
            String[] parms = getList(areas[i]);
            for(int j=0; j<parms.length; j++){
                if(parms[j].equalsIgnoreCase(ConfigParameters.pPASSWORD)) {
                    logger.info(areas[i] + ":" + parms[j] + ":" + "********");
                } else {
                    logger.info(areas[i] + ":" + parms[j] + ":" + get(areas[i],parms[j]));
                }
            }
        }
        logger.info("============================================================================");
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer(2048);
        String[] areas = getList();
        for(int i=0; i<areas.length; i++) {
            String[] parms = getList(areas[i]);
            for(int j=0; j<parms.length; j++){
                if(parms[j].equalsIgnoreCase(ConfigParameters.pPASSWORD)) {
                    buffer.append(areas[i] + ":" + parms[j] + ":" + "********\n");
                } else {
                    buffer.append(areas[i] + ":" + parms[j] + ":" + get(areas[i],parms[j]) + "\n");
                }
            }
        }

        return buffer.toString();
    }
}
