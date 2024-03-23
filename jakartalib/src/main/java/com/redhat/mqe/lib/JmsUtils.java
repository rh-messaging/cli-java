/*
 * Copyright (c) 2021 Red Hat, Inc.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.redhat.mqe.lib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.jms.BytesMessage;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.StreamMessage;
import jakarta.jms.TextMessage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class JmsUtils {
    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

    /**
     * Calculate TTL of given message from message
     * expiration time and message timestamp.
     * <p/>
     * Returns the time the message expires, which is the sum of the time-to-live value
     * specified by the client and the GMT at the time of the send
     * EXP_TIME = CLIENT_SEND+TTL (CLIENT_SEND??)
     * CLIENT_SEND time is approximately getJMSTimestamp() (time value between send()/publish() and return)
     * TODO - check for correctness
     *
     * @param message calculate TTL for this message
     * @return positive long number if TTL was calculated. Long.MIN_VALUE if error.
     */
    public static long getTtl(Message message) {
        long ttl = 0;
        try {
            long expiration = message.getJMSExpiration();
            long timestamp = message.getJMSTimestamp();
            if (expiration != 0 && timestamp != 0) {
                ttl = expiration - timestamp;
            }
        } catch (JMSException jmse) {
            LOG.error("Error while calculating TTL value.\n" + jmse.getMessage());
            jmse.printStackTrace();
            System.exit(1);
        }
        return ttl;
    }

    public static void streamMessageContentToFile(String filePath, Message message, int msgCounter) {
        try {
            File outputFile = getFilePath(filePath, msgCounter);
            try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
                 BufferedOutputStream bufferedOutput = new BufferedOutputStream(fileOutputStream)) {
//                final String saveStream = "JMS_AMQ_SaveStream";
                final String saveStream = "JMS_AMQ_OutputStream";
                message.setObjectProperty(saveStream, bufferedOutput);
            }
        } catch (IOException e) {
            LOG.error("Error while writing to file '" + filePath + "'.");
            e.printStackTrace();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    static File getFilePath(String filePath, int msgCounter) throws IOException {
        File file;
        if (filePath == null || filePath.isEmpty()) {
            file = File.createTempFile("recv_msg_", Long.toString(System.currentTimeMillis()));
        } else {
            file = new File(filePath + "_" + msgCounter);
        }
        return file;
    }

    /**
     * Write message body (text or binary) to provided file or default one in temp directory.
     *
     * @param filePath file to write data to
     * @param message  to be read and written to provided file
     */
    public static void writeMessageContentToFile(String filePath, Message message, int msgCounter) {
        byte[] readByteArray;
        try {
            File file;
            file = getFilePath(filePath, msgCounter);

            LOG.debug("Write message content to file '" + file.getPath() + "'.");
            if (message instanceof BytesMessage) {
                LOG.debug("Writing BytesMessage to file");
                BytesMessage bm = (BytesMessage) message;
                readByteArray = new byte[(int) bm.getBodyLength()];
                bm.reset(); // added to be able to read message content
                bm.readBytes(readByteArray);
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(readByteArray);
                }
            } else if (message instanceof StreamMessage) {
                LOG.debug("Writing StreamMessage to file");
                StreamMessage sm = (StreamMessage) message;
//        sm.reset(); TODO haven't tested this one
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(sm.readObject());
                oos.close();
            } else if (message instanceof TextMessage) {
                LOG.debug("Writing TextMessage to file");
                try (FileWriter fileWriter = new FileWriter(file)) {
                    TextMessage tm = (TextMessage) message;
                    fileWriter.write(tm.getText());
                }
            }
        } catch (JMSException e) {
            e.printStackTrace();
        } catch (IOException e1) {
            LOG.error("Error while writing to file '" + filePath + "'.");
            e1.printStackTrace();
        }
    }
}
