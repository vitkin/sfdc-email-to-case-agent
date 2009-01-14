/*
 * Copyright (c) 2006, salesforce.com, inc.
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

/**
 * Collection of all the config parameters used in sfdcConfig.txt and email2case.txt
 *
 */
public interface ConfigParameters {

    //Sections
    final String pLOGIN           = "sfdcLogin";
    final String pNOTIFY          = "notify";
    final String pATTACH          = "attachments";
    final String pSERVICES        = "services";
    final String pADMIN           = "admin";

    final String pTIMEOUT         = "timeout";
    final String pURL             = "url";
    final String pLOGIN_REFRESH   = "loginRefresh";
    final String pNOTIFY_EMAIL    = "notifyEmail";
    final String pNOTIFY_ON_ERROR = "notifyonerror";
    final String pFROM            = "from";

    final String pUSERNAME        = "userName";
    final String pPASSWORD        = "password";
    final String pPORT            = "port";
    final String pHOST            = "host";
    final String pUSER            = "user";
    final String pSERVICE         = "service";
    final String pSFDC_LOGIN      = "sfdcLogin";
    final String pPROTOCOL        = "protocol";
    final String pINTERVAL        = "interval";
    final String pINBOX           = "inbox";
    final String pREADBOX         = "readbox";
    final String pERRORBOX        = "errorbox";

    final String pATTACH_DIR      = "largeAttachmentDirectory";
    final String pATTACH_URL      = "largeAttachmentURLPrefix";
    final String pATTACH_SIZE     = "largeAttachmentSize";
}
