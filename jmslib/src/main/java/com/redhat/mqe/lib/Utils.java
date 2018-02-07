/*
 * Copyright (c) 2017 Red Hat, Inc.
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

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.StreamMessage;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * Utils class provides static methods for various
 * usages.
 */

public class Utils {
    private static Logger LOG = LoggerFactory.getLogger(Utils.class);

    public static final List<? extends Class<?>> CLASSES = Collections.unmodifiableList(Arrays.asList(
        Integer.class, Long.class, Float.class, Double.class, Boolean.class, String.class));
    // todo Short.class, Byte.class ?

    /**
     * Remove leading and trailing quotation from arguments.
     *
     * @param argument to have dropped quotes (if has)
     * @return argument without leading/trailing quote(s)
     */
    public static String removeQuotes(String argument) {
        if (argument.startsWith("\"") && argument.endsWith("\"")
            || argument.startsWith("'") && argument.endsWith("'")) {
            return argument.substring(1, argument.length() - 1);
        } else {
            return argument;
        }
    }

    /**
     * Sleep for given period of time.
     *
     * @param miliseconds time to sleep
     */
    public static void sleep(long miliseconds) {
        if (miliseconds > 1) {
            try {
                Thread.sleep(miliseconds);
            } catch (InterruptedException e) {
                LOG.error("Exception while sleeping!\n" + e.getMessage());
                e.printStackTrace();
                System.exit(2);
            }
        }
    }

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

    /**
     * @return number of seconds (including milisecs) since EPOCH.
     */
    public static double getTime() {
        return System.currentTimeMillis();
    }

    /**
     * Sleeps until next timed for/while loop iteration.
     * This method takes into account the length of the action it preceded.
     *
     * @param initialTimestamp initial timestamp (in get_time() double form) is passed from a connection started
     * @param msgCount         number of iterations
     * @param duration         total time of all iterations
     * @param nextCountIndex   next iteration index
     */

    public static void sleepUntilNextIteration(double initialTimestamp, int msgCount, double duration, int nextCountIndex) {
        if ((duration > 0) && (msgCount > 0)) {
            // initial overall duration approximation of whole loop (sender/receiver)
            double cummulative_dur = (1.0 * nextCountIndex * duration) / msgCount;
            while (true) {
                if (getTime() - initialTimestamp - cummulative_dur > -0.05)
                    break;
                try {
                    LOG.trace("sleeping");
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Set logging level to given value. If not explicitly
     * specified, default level is used from *logger* properties file.
     * (As of the time writing - simplelogger.properties is used as default.)
     * <p/>
     * NOTE: SLF4J is not capable of changing log levels programatically!
     * We have to change the System/File property of given underlying logger.
     *
     * @param logLevel logging level to be logger set to
     */
    public static void setLogLevel(String logLevel) {
        org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger("com.redhat.mqe.jms");
        Level level;
        switch (logLevel.toLowerCase()) {
            case "all":
                level = Level.ALL;
                break;
            case "trace":
                level = Level.TRACE;
                break;
            case "debug":
                level = Level.DEBUG;
                break;
            case "info":
                level = Level.INFO;
                break;
            case "warn":
                level = Level.WARN;
                break;
            case "error":
                level = Level.ERROR;
                break;
            case "fatal":
                level = Level.FATAL;
                break;
            case "off":
                level = Level.OFF;
                break;
            default:
                level = Level.INFO;
        }
        LogManager.getRootLogger().setLevel(level);
        logger.setLevel(level);
    }

    /**
     * Try to find out the type of the parameter.
     * If preferredStringType is provided, given class is returned.
     * Else start retypying with Integer, Long, Float, Double, Boolean
     * and if still not found the correct type, use String
     * as the default.
     *
     * @param preferredStringType type of the preferred (suggested) type given as string
     * @param value              to be find out its type
     * @return Class type of given value
     */

    public static Class<?> getClassType(String preferredStringType, String value, boolean allowExplicitRetype) throws JmsMessagingException {
        LOG.trace("getClassType for " + value + ":" + preferredStringType);
        if (preferredStringType == null) {
            // set manually the contentType to (autotypecasting)
            if (value.startsWith("~") && allowExplicitRetype) {
                LOG.trace("Auto-typecasting " + value);
                value = value.substring(1);
                // try to auto-typecast
                for (Class<?> clazz : CLASSES) {
                    try {
                        // By this method we will found out the type of the object. Only few are supported
                        getObjectValue(clazz, value, true);
                        return clazz;
                    } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                        LOG.trace("Ok, the type of the object is not " + clazz + ". Try another one");
                    }
                }
            } else {
                // We should default to use string
                preferredStringType = "string";
            }
        }
        if (preferredStringType == null) {
            LOG.error("Error while finding out the content data type.");
            System.exit(2);
        }
        switch (preferredStringType.toLowerCase()) {
            case "int":
            case "integer":
                preferredStringType = "Integer";
                break;
            case "long":
                preferredStringType = "Long";
                break;
            case "float":
                preferredStringType = "Float";
                break;
            case "double":
                preferredStringType = "Double";
                break;
            case "bool":
            case "boolean":
                preferredStringType = "Boolean";
                break;
            case "string":
            default:
                preferredStringType = "String";
                break;
        }
        try {
            LOG.trace("Using " + preferredStringType + " for " + value);
            return Class.forName("java.lang." + preferredStringType);
        } catch (ClassNotFoundException e) {
            LOG.error("Error while finding out the type of the object.");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Method returns the primitive class type for given class.
     * (Unboxing technique: Integer.class -> int.class, Float.class -> float.class)
     * String class is not affected, because it is "special"..
     * This is done only for Message.setXProperty(String.class, <primitive-type>).
     *
     * @param clazz primitive Class type to be changed (unboxed)
     * @return primitive class type from clazz
     */
    public static Class<?> getPrimitiveClass(Class<?> clazz) {
        if (CLASSES.contains(clazz)) {
            Class<?> primitiveName;
            switch (clazz.getSimpleName()) {
                case "Integer":
                    primitiveName = int.class;
                    break;
                case "Long":
                    primitiveName = long.class;
                    break;
                case "Float":
                    primitiveName = float.class;
                    break;
                case "Double":
                    primitiveName = double.class;
                    break;
                case "Boolean":
                    primitiveName = boolean.class;
                    break;
                case "String":
                default:
                    primitiveName = String.class;
                    break;
            }
            return primitiveName;
        }
        LOG.error("Error!" + clazz + " is not a primitive Class type! Exiting.");
        System.exit(2);
        return null;
    }

    /**
     * Try to call a parsing method 'valueOf()' on given Object.
     * If it is String.class, use 'Object' as an argument,
     * else parse from String value.
     *
     * @param clazz  try to cast object to this class type
     * @param object to be casted to specified clazz
     * @return Object instance of given type (can be subtyped to given object, if needed)
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    @SuppressWarnings("unchecked")
    public static Object getObjectValue(Class<?> clazz, String object, boolean allowExplicitRetype) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (object.startsWith("~") && allowExplicitRetype) {
            object = object.replaceFirst("~", "");
        }
        Class<?> argType;
        // if it is a subclass of Number
        if (Number.class.isAssignableFrom(clazz)
            || object.toLowerCase().equals("true") || object.toLowerCase().equals("false")) {
            argType = String.class;
        } else {
            argType = Object.class;
        }
        Method method = clazz.getMethod("valueOf", argType);
        Object myObj = clazz.cast(method.invoke(clazz, object));
        return myObj;
    }

    public static void streamBinaryContentToFile(String filePath, Message message, int msgCounter) {
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

    /**
     * Write message binary body to provided file or default one in temp directory.
     *
     * @param filePath file to write data to
     * @param message  to be read and written to provided file
     */
    public static void writeBinaryContentToFile(String filePath, Message message, int msgCounter) {
        byte[] readByteArray;
        try {
            File writeBinaryFile;
            writeBinaryFile = getFilePath(filePath, msgCounter);

            LOG.debug("Write binary content to file '" + writeBinaryFile.getPath() + "'.");
            if (message instanceof BytesMessage) {
                BytesMessage bm = (BytesMessage) message;
                readByteArray = new byte[(int) bm.getBodyLength()];
                bm.reset(); // added to be able to read message content
                bm.readBytes(readByteArray);
                try (FileOutputStream fos = new FileOutputStream(writeBinaryFile)) {
                    fos.write(readByteArray);
                    fos.close();
                }

            } else if (message instanceof StreamMessage) {
                LOG.debug("Writing StreamMessage to");
                StreamMessage sm = (StreamMessage) message;
//        sm.reset(); TODO haven't tested this one
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(sm.readObject());
                oos.close();
            }

        } catch (JMSException e) {
            e.printStackTrace();
        } catch (IOException e1) {
            LOG.error("Error while writing to file '" + filePath + "'.");
            e1.printStackTrace();
        }
    }

    private static File getFilePath(String filePath, int msgCounter) throws IOException {
        File writeBinaryFile;
        if (filePath == null || filePath.equals("")) {
            writeBinaryFile = File.createTempFile("recv_msg_", Long.toString(System.currentTimeMillis()));
        } else {
            writeBinaryFile = new File(filePath + "_" + msgCounter);
        }
        return writeBinaryFile;
    }
}
