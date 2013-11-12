/*
 * #%L
 * sfdc-email-to-case-agent
 * %%
 * Copyright (C) 2006 salesforce.com, inc.
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
package com.sforce.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 *
 * ConsoleReader
 *
 * Utility class used to read input from the console.
 *
 */
public class ConsoleReader {
    private static final String pENTER          = "Enter ";

    /**
     * Read input from console while echoing the user's keystrokes.
     *
     * @param sPrompt Prompt shown on console for user input
     * @return user input or empty string if and exception occurs
     */
    public static String readArgumentFromConsole(final String sPrompt) {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String sArgument = "";
        try {
            System.out.print(pENTER + sPrompt + ": ");
            sArgument = in.readLine();
        } catch ( IOException ioe ) {
            return "";
        }
        return sArgument;
    }

    /**
     * Read input from console while erasing the user's keystrokes.
     * particularly useful for server type applications running in
     * a console window
     *
     * @param sPrompt Prompt shown on console for user input
     * @return user input or empty string if and exception occurs
     */
    public static String readPasswordFromConsole (final String sPrompt) throws IOException {
        ConsoleEraser eraser = new ConsoleEraser ( pENTER + sPrompt + ": ");
        eraser.start ();
        String password = "";
        while (true) {
            char c = (char)System.in.read();
            // assume enter pressed, stop masking
            eraser.stopMasking();

            if (c == '\r') {
               c = (char)System.in.read();
               if (c == '\n') {
                  break;
               } else {
                  continue;
               }
            } else if (c == '\n') {
               break;
            } else {
               // store the password
               password += c;
            }
         }
        return password;
    }


    private static class ConsoleEraser extends Thread {
        private boolean stop = false;
        private String prompt;


       /**
        *@param prompt The prompt displayed to the user
        */
        public ConsoleEraser(String prompt) {
           this.prompt = prompt;
        }

       /**
        * Mask input until told to stop
        */
        @Override
        public void run() {
           while(!stop) {
              try {
                 // attempt masking at this rate
                 Thread.sleep(1);
              }catch (InterruptedException iex) {
                 iex.printStackTrace();
              }
              if (!stop) {
                 System.out.print("\r" + prompt + " \r" + prompt);
              }
              System.out.flush();
           }
        }

       /**
        * Instruct the thread to stop masking.
        */
        public void stopMasking() {
           this.stop = true;
        }
    }

}
