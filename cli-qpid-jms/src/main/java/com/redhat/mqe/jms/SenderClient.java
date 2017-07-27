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

package com.redhat.mqe.jms;

import com.redhat.mqe.lib.Utils;
import com.redhat.mqe.lib.Content;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.List;
import javax.jms.*;

/**
 * SenderClient is able to send various messages with wide options
 * of settings of these messages.
 */
public class SenderClient extends CoreClient {
  private SenderOptions senderOptions;
  private static List<Content> content;
  private static boolean isEmptyMessage = false;
  private boolean userMessageCounter = false;
  private String userMessageCounterText;
  private static byte[] binaryMessageData;
  static final String QPID_SUBJECT = "qpid.subject";
  static final String AMQ_SUBJECT = "JMS_AMQP_Subject";
  static final String BEFORE_SEND = "before-send";
  static final String AFTER_SEND = "after-send";
  static final String AFTER_SEND_TX_ACTION = "after-send-tx-action";


  public SenderClient(String[] arguments) {
    senderOptions = new SenderOptions();
    ClientOptionManager.applyClientArguments(senderOptions, arguments);
  }

  /**
   * Initial method to start the client.
   * Initialization of content, properties and everything about
   * how to send message is done/initiated by this method.
   * Contains the main sending loop method.
   */
  void startClient() {
    ClientOptions senderOptions = this.getClientOptions();
    setGlobalClientOptions(senderOptions);
    Connection connection = this.createConnection(senderOptions);

    // Transactions support
    int transactionSize = 0;
    String transaction = null;
    if (senderOptions.getOption(ClientOptions.TX_SIZE).hasParsedValue()
        || senderOptions.getOption(ClientOptions.TX_ENDLOOP_ACTION).hasParsedValue()) {
      transactionSize = Integer.parseInt(senderOptions.getOption(ClientOptions.TX_SIZE).getValue());
      if (senderOptions.getOption(ClientOptions.TX_ACTION).hasParsedValue()) {
        transaction = senderOptions.getOption(ClientOptions.TX_ACTION).getValue().toLowerCase();
      } else {
        transaction = senderOptions.getOption(ClientOptions.TX_ENDLOOP_ACTION).getValue().toLowerCase();
      }
    }

    try {
      Session session = (transaction == null || transaction.equals("none")) ?
          this.createSession(senderOptions, connection, false) : this.createSession(senderOptions, connection, true);
      connection.start();
      MessageProducer msgProducer = session.createProducer(this.getDestination());
      setMessageProducer(senderOptions, msgProducer);

      // check if not empty message
      if (!(isEmptyMessage = checkForEmptyMessage(senderOptions))) {
        // Create message content from provided input
        createMessageContent(senderOptions);
      } else {
        content = new ArrayList<>();
      }

      // Calculate msg-rate from COUNT & DURATION
      double initialTimestamp = Utils.getTime();
      int count = Integer.parseInt(senderOptions.getOption(ClientOptions.COUNT).getValue());
      double duration = Double.parseDouble(senderOptions.getOption(ClientOptions.DURATION).getValue());
      duration *= 1000;  // convert to milliseconds

      // Create message and fill body with data (content)
      Message message = this.createMessage(senderOptions, session);
      this.setMessageProperties(message);
      this.setCustomMessageProperties(message);

      int msgCounter = 0;
      String durationMode = senderOptions.getOption(ClientOptions.DURATION_MODE).getValue();
      while (true) {
        // Set user defined message auto counter
        if (userMessageCounter && (message instanceof TextMessage)) {
          ((TextMessage) message).setText(String.format(userMessageCounterText, msgCounter));
        }
        // TODO Set variable message properties - have not found any so far..
        // sleep for given amount of time, defined by msg-rate "before-send"
        if (durationMode.equals(BEFORE_SEND)) {
          LOG.trace("Sleeping before send");
          Utils.sleepUntilNextIteration(initialTimestamp, count, duration, msgCounter + 1);
        }

        // Send messages
        msgProducer.send(message);
        msgCounter++;
        // Makes message body read only from write only mode
        if (message instanceof StreamMessage) {
          ((StreamMessage) message).reset();
        }
        if (message instanceof BytesMessage) {
          ((BytesMessage) message).reset();
        }
        printMessage(senderOptions, message);
        // sleep for given amount of time, defined by msg-rate "after-send-before-tx-action"
        if (durationMode.equals(AFTER_SEND)) {
          LOG.trace("Sleeping after send");
          Utils.sleepUntilNextIteration(initialTimestamp, count, duration, msgCounter + 1);
        }

        // TX support
        if (transaction != null && transactionSize != 0) {
          if (msgCounter %  transactionSize == 0) {
            // Do transaction action
            doTransaction(session, transaction);
          }
        }
        // sleep for given amount of time, defined by msg-rate "after-send-after-tx-action"
        if (durationMode.equals(AFTER_SEND_TX_ACTION)) {
          LOG.trace("Sleeping after send & tx action");
          Utils.sleepUntilNextIteration(initialTimestamp, count, duration, msgCounter + 1);
        }
        if (count == 0) continue;
        if (msgCounter == count) break;
      }

      // Finish transaction with sending of the rest messages
      if (transaction != null) {
        doTransaction(session, senderOptions.getOption(ClientOptions.TX_ENDLOOP_ACTION).getValue());
      }
    } catch (JMSException | IllegalArgumentException jmse) {
      LOG.error("Error while sending a message!", jmse.getMessage());
      jmse.printStackTrace();
      System.exit(1);
    } finally {
      double closeSleep = Double.parseDouble(this.getClientOptions().getOption(ClientOptions.CLOSE_SLEEP).getValue());
      closeConnObjects(this, closeSleep);
      this.close(connection);
    }
  }

  /**
   * Set default priority, ttl, durability and creating of id,
   * timestamps for messages of this message producer.
   *
   * @param senderOptions specify defined options for messages & messageProducers
   * @param producer      set this message producer
   */
  private static void setMessageProducer(ClientOptions senderOptions, MessageProducer producer) {
    try {
      // set delivery mode - durable/non-durable
      String deliveryModeArg = senderOptions.getOption(ClientOptions.MSG_DURABLE).getValue().toLowerCase();
      int deliveryMode = (deliveryModeArg.equals("true") || deliveryModeArg.equals("yes"))
          ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT;
      producer.setDeliveryMode(deliveryMode);
      // set time to live of message if provided
      if (senderOptions.getOption(ClientOptions.MSG_TTL).hasParsedValue()) {
        producer.setTimeToLive(Long.parseLong(senderOptions.getOption(ClientOptions.MSG_TTL).getValue()));
      }
      // set message priority if provided
      if (senderOptions.getOption(ClientOptions.MSG_PRIORITY).hasParsedValue()) {
        int priority = Integer.parseInt(senderOptions.getOption(ClientOptions.MSG_PRIORITY).getValue());
        if (priority < 0 || priority > 10) {
          LOG.warn("Message priority is not in JMS interval <0, 10>.");
        }
        producer.setPriority(priority);
      }
      // Set Message ID or disable it completely
      if (senderOptions.getOption(ClientOptions.MSG_ID).hasParsedValue()) {
        if (senderOptions.getOption(ClientOptions.MSG_ID).getValue().equals("noid")) {
          producer.setDisableMessageID(true);
        }
      }
      // Producer does not generate timestamps - for performance only
      producer.setDisableMessageTimestamp(
          Boolean.parseBoolean(senderOptions.getOption(ClientOptions.MSG_NOTIMESTAMP).getValue()));
    } catch (JMSException e) {
      e.printStackTrace();
    }
  }

  /**
   * Method creates message based on provided content on given session.
   *
   * @param senderOptions specify defined options for messages & messageProducers
   * @param session       to which message will belong
   * @return newly created and set up message to be sent
   */
  @SuppressWarnings("unchecked")
  private <T extends Message> T createMessage(ClientOptions senderOptions, Session session) {
    try {
      if (senderOptions.getOption(com.redhat.mqe.lib.ClientOptions.MSG_CONTENT_BINARY).hasParsedValue()) {
        BytesMessage bytesMessage = session.createBytesMessage();
        fillBytesMessage(senderOptions, bytesMessage);
        return (T) bytesMessage;
      } else if (senderOptions.getOption(ClientOptions.MSG_CONTENT).hasParsedValue()
          || senderOptions.getOption(ClientOptions.MSG_CONTENT_FROM_FILE).hasParsedValue()) {
//        if (senderOptions.getOption(ClientOptions.CONTENT_TYPE).hasParsedValue()) {
        String textContent;
        switch (senderOptions.getOption(ClientOptions.CONTENT_TYPE).getValue().toLowerCase()) {
          case "string":
            TextMessage textMessage = session.createTextMessage();
            textContent = content.get(0).getValue().toString();
            String pattern = "%[ 0-9]*d";
            Pattern r = Pattern.compile(pattern);
            Matcher matcher = r.matcher(textContent);

            if (matcher.find()) {
              userMessageCounter = true;
              userMessageCounterText = textContent;
            }
            textMessage.setText(textContent);
            return (T) textMessage;
          case "object":
          case "int":
          case "integer":
          case "long":
          case "float":
          case "double":
          case "bool":
            ObjectMessage objectMessage = session.createObjectMessage();
            fillObjectMessage(senderOptions, objectMessage);
            return (T) objectMessage;
        }
//        }
      } else if (senderOptions.getOption(ClientOptions.MSG_CONTENT_LIST_ITEM).hasParsedValue()) {
        // Create "ListMessage" using StreamMessage
        StreamMessage pseudoListMessage = session.createStreamMessage();
        for (Content c : content) {
          pseudoListMessage.writeObject(c.getValue());
        }
        return (T) pseudoListMessage;
      } else if (senderOptions.getOption(ClientOptions.MSG_CONTENT_MAP_ITEM).hasParsedValue()) {
        MapMessage mapMessage = session.createMapMessage();
        if (!isEmptyMessage) {
          fillMapMessage(senderOptions, mapMessage);
        }
        return (T) mapMessage;
      } else {
        LOG.trace("Unknown type of message should be created! Sending empty Message");
        return (T) session.createMessage();
      }
    } catch (JMSException e) {
      LOG.error("Error while creating message or setting content to this message!");
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Create & return empty Map/ListMessage
   *
   * @param senderOptions options of the sender
   * @return empty MapMessage or ListMessage
   */
  private static boolean checkForEmptyMessage(ClientOptions senderOptions) {
    List<String> values;
    if (senderOptions.getOption(ClientOptions.MSG_CONTENT_LIST_ITEM).hasParsedValue()) {
      values = senderOptions.getOption(ClientOptions.MSG_CONTENT_LIST_ITEM).getParsedValuesList();
      if (isEmptyMessage(values)) {
        return true;
      }
    } else if (senderOptions.getOption(ClientOptions.MSG_CONTENT_MAP_ITEM).hasParsedValue()) {
      values = senderOptions.getOption(ClientOptions.MSG_CONTENT_MAP_ITEM).getParsedValuesList();
      if (isEmptyMessage(values)) {
        // createEmptyListMessage;
        return true;
      }
    }
    return false;
  }

  /**
   * Returns whether the list of Strings is empty.
   *
   * @param values list to be checked for emptiness
   * @return true if the list is empty, false otherwise
   */
  private static boolean isEmptyMessage(List<String> values) {
    return (values.size() == 1
        && (values.get(0).equals("") || values.get(0).equals("\"\"") || values.get(0).equals("\'\'")));
  }

  /**
   * Fill object message with data.
   *
   * @param senderOptions
   * @param objectMessage
   */
  private void fillObjectMessage(ClientOptions senderOptions, ObjectMessage objectMessage) {
    try {
      LOG.debug("Filling object data");
      if (content.size() == 1) {
        objectMessage.setObject((Serializable) content.get(0).getValue());
      } else {
        LOG.error("Content is bigger then one object. " +
            "Don't know how to set for object message multiple data. Use ListMessage(?)");
        System.exit(2);
      }
    } catch (JMSException e) {
      e.printStackTrace();
    }
  }

  /**
   * Fill MapMessage with a data provided by user input as *content*.
   *
   * @param senderOptions sender options
   * @param mapMessage    message to be filled with data
   */
  private void fillMapMessage(ClientOptions senderOptions, MapMessage mapMessage) {
    for (Content c : content) {
      LOG.trace("Filling MapMessage with: " + c.getValue() + " class=" + c.getType().getName());
      if (Utils.CLASSES.contains(c.getType())) {
        try {
          switch (c.getType().getSimpleName()) {
            case "Integer":
              mapMessage.setInt(c.getKey(), (Integer) c.getValue());
              break;
            case "Long":
              mapMessage.setLong(c.getKey(), (Long) c.getValue());
              break;
            case "Float":
              mapMessage.setFloat(c.getKey(), (Float) c.getValue());
              break;
            case "Double":
              mapMessage.setDouble(c.getKey(), (Double) c.getValue());
              break;
            case "Boolean":
              mapMessage.setBoolean(c.getKey(), (Boolean) c.getValue());
              break;
            case "String":
              mapMessage.setString(c.getKey(), (String) c.getValue());
              break;
            default:
              LOG.error("Sending unknown type element!");
              mapMessage.setObject(c.getKey(), c.getValue());
          }
          mapMessage.setObject(c.getKey(), c.getValue());
        } catch (JMSException e) {
          LOG.error("Error while setting MapMessage property\n" + e.getMessage());
          e.printStackTrace();
          System.exit(1);
        }
      } else {
        LOG.error("Unknown data type in message Content. Do not know how to send it. Type=" + c.getType());
      }
    }
  }

  /**
   * Write bytes to BytesMessage
   * @param senderOptions
   * @param bytesMessage
   */
  private void fillBytesMessage(ClientOptions senderOptions, BytesMessage bytesMessage) {
    LOG.debug("Filling ByteMessage with binary data");
    try {
      bytesMessage.writeBytes(binaryMessageData);
    } catch (JMSException e) {
      e.printStackTrace();
    }
  }

  /**
   * Set various JMS (header) properties for this message.
   *
   * @param message to set properties for
   * @return same message with updated properties
   */
  private Message setMessageProperties(Message message) {
    try {
      // Set message ID if provided or use default one
      if (senderOptions.getOption(ClientOptions.MSG_ID).hasParsedValue()) {
        message.setJMSMessageID(senderOptions.getOption(ClientOptions.MSG_ID).getValue());
      }
      // Set message Correlation ID
      if (senderOptions.getOption(ClientOptions.MSG_CORRELATION_ID).hasParsedValue()) {
        message.setJMSCorrelationID(senderOptions.getOption(ClientOptions.MSG_CORRELATION_ID).getValue());
      }

      // Set message Subject
      if (senderOptions.getOption(ClientOptions.MSG_SUBJECT).hasParsedValue()) {
        message.setJMSType(senderOptions.getOption(ClientOptions.MSG_SUBJECT).getValue());
      }
      // Set message reply to destination (queue only for now)
      if (senderOptions.getOption(ClientOptions.MSG_REPLY_TO).hasParsedValue()) {
        // TODO only queue is implemented for now - how to distinguish topic X queue? prefix topic://<topic>
        Destination destination = this.getSessions().get(0)
            .createQueue(senderOptions.getOption(ClientOptions.MSG_REPLY_TO).getValue());
        message.setJMSReplyTo(destination);
      }

      // Set message type to message content type (some JMS vendors use this internally)
      if (senderOptions.getOption(ClientOptions.MSG_CONTENT_TYPE).hasParsedValue()) {
        message.setStringProperty("JMS_AMQP_CONTENT_TYPE", senderOptions.getOption(ClientOptions.MSG_CONTENT_TYPE).getValue());
      }
      // Set message priority (4 by default)
      if (senderOptions.getOption(ClientOptions.MSG_PRIORITY).hasParsedValue()) {
        message.setJMSPriority(Integer.parseInt(senderOptions.getOption(ClientOptions.MSG_PRIORITY).getValue()));
      }

      // Set the group the message belongs to
      if (senderOptions.getOption(ClientOptions.MSG_GROUP_ID).hasParsedValue()) {
        message.setStringProperty("JMSXGroupID", senderOptions.getOption(ClientOptions.MSG_GROUP_ID).getValue());
      }
      // Set relative position of this message within its group
      if (senderOptions.getOption(ClientOptions.MSG_GROUP_SEQ).hasParsedValue()) {
        message.setStringProperty("JMSXGroupSeq", senderOptions.getOption(ClientOptions.MSG_GROUP_SEQ).getValue());
      }

      // JMS AMQP specific reply-to-group-id mapping
      if (senderOptions.getOption(ClientOptions.MSG_REPLY_TO_GROUP_ID).hasParsedValue()) {
        message.setStringProperty("JMS_AMQP_REPLY_TO_GROUP_ID", senderOptions.getOption(ClientOptions.MSG_REPLY_TO_GROUP_ID).getValue());
      }

    } catch (JMSException e) {
      e.printStackTrace();
    }
    return message;
  }

  /**
   * Set custom property values in the JMS header/body.
   * Setting is done using reflection on Message object and invoking
   * appropriate setXProperty(String, <primitive-type>).
   *
   * @param message message to have properties updated
   */
  private void setCustomMessageProperties(Message message) {
    String globalPropertyType = null;
    if (senderOptions.getOption(ClientOptions.PROPERTY_TYPE).hasParsedValue()) {
      globalPropertyType = senderOptions.getOption(ClientOptions.PROPERTY_TYPE).getValue();
    }
    if (senderOptions.getOption(ClientOptions.MSG_PROPERTY).hasParsedValue()) {
      List<String> customProperties = senderOptions.getOption(ClientOptions.MSG_PROPERTY).getParsedValuesList();
      for (String property : customProperties) {
        // Create new 'content' object for property key=value mapping. It is same as Message Content Map, so we can safely reuse
        Content propertyContent = new Content(globalPropertyType, property, true);
        try {
          String simpleName = propertyContent.getType().getSimpleName();
          if (simpleName.equals("Integer")) {
            simpleName = "Int";
          }
          // Call the appropriate setXProperty(String, <primitive-type>) - complicated with primitive types..
          LOG.trace("calling method \"set" + simpleName + "Property()\" for " + property);
          Method messageSetXPropertyMethod = Message.class.getMethod("set" + simpleName + "Property",
              String.class, Utils.getPrimitiveClass(propertyContent.getType()));
          messageSetXPropertyMethod.invoke(message, propertyContent.getKey(), propertyContent.getValue());
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
          LOG.error("Unable to set message property from provided input. Exiting.");
          e.printStackTrace();
          System.exit(2);
        }
      }
    }
  }


  public List<Content> getContent() {
    return content;
  }

  @Override
  ClientOptions getClientOptions() {
    return senderOptions;
  }


  /**
   * Create message Content based on provided input.
   * This method does not care about types. Creation of object Content
   * takes care of autotype-casting and setting the proper values.
   *
   * @param senderOptions use provided input option
   * @return list of created options with at least one value
   */
  static void createMessageContent(ClientOptions senderOptions) {
    List<Content> contentList = new ArrayList<>();
    String globalContentType = null;
    // Set global content value
    if (senderOptions.getOption(ClientOptions.CONTENT_TYPE).hasParsedValue()) {
      globalContentType = senderOptions.getOption(ClientOptions.CONTENT_TYPE).getValue().toLowerCase();
    }
    // Set content stuff
    if (senderOptions.getOption(ClientOptions.MSG_CONTENT).hasParsedValue()) {
      LOG.trace("set MSG_CONTENT");
      String value = senderOptions.getOption(ClientOptions.MSG_CONTENT).getValue();
      contentList.add(new Content(globalContentType, value, false));
    } else if (senderOptions.getOption(ClientOptions.MSG_CONTENT_LIST_ITEM).hasParsedValue()) {
      LOG.trace("set MSG_CONTENT_LIST_ITEM");
      List<String> values = senderOptions.getOption(ClientOptions.MSG_CONTENT_LIST_ITEM).getParsedValuesList();
      for (String parsedItem : values) {
        contentList.add(new Content(globalContentType, parsedItem, false));
      }
    } else if (senderOptions.getOption(ClientOptions.MSG_CONTENT_MAP_ITEM).hasParsedValue()) {
      LOG.trace("set MSG_CONTENT_MAP_ITEM");
      List<String> values = senderOptions.getOption(ClientOptions.MSG_CONTENT_MAP_ITEM).getParsedValuesList();
      for (String parsedItem : values) {
        contentList.add(new Content(globalContentType, parsedItem, true));
      }
    } else if (senderOptions.getOption(ClientOptions.MSG_CONTENT_FROM_FILE).hasParsedValue()) {
      LOG.trace("set MSG_CONTENT_FROM_FILE");
      if (senderOptions.getOption(ClientOptions.MSG_CONTENT_BINARY).hasParsedValue()
          && Boolean.parseBoolean(senderOptions.getOption(ClientOptions.MSG_CONTENT_BINARY).getValue())) {
        binaryMessageData = readBinaryContentFromFile(senderOptions.getOption(ClientOptions.MSG_CONTENT_FROM_FILE).getValue());
      } else {
        String text = readContentFromFile(senderOptions.getOption(ClientOptions.MSG_CONTENT_FROM_FILE).getValue());
        contentList.add(new Content(globalContentType, text, false));
      }
    }
    content = contentList;
  }


  /**
   * Read binary content from file
   * @param binaryFileName binary file to be read from
   * @return read Byte array from file
   */
  private static byte[] readBinaryContentFromFile(String binaryFileName) {
    File binaryFile = new File(binaryFileName);
    byte[] bytesOut = null;
    if (binaryFile.canRead()) {
      bytesOut = new byte[(int) binaryFile.length()];
      try {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(binaryFile))) {
          int totalBytesRead = 0;
          while (totalBytesRead < bytesOut.length) {
            int bytesRemaining = bytesOut.length - totalBytesRead;
            //input.read() returns -1, 0, or more :
            int bytesRead = bis.read(bytesOut, totalBytesRead, bytesRemaining);
            if (bytesRead > 0) {
              totalBytesRead = totalBytesRead + bytesRead;
            }
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    } else {
      LOG.error("Unable to access file " + binaryFileName);
      System.exit(2);
    }
    LOG.debug("ToSend=" + new String(bytesOut));
    return bytesOut;
  }

  /**
   * Read content from provided file path. File content is returned
   * as a string representation of all lines.
   *
   * @param path path to file to read input from
   * @return the concatenad
   */
  private static String readContentFromFile(String path) {
    StringBuilder fileContent = new StringBuilder();
    try {
      Path filePath = Paths.get(path);
      if (Files.exists(filePath) && Files.isReadable(filePath)) {
        for (String line : Files.readAllLines(filePath, Charset.defaultCharset())) {
          fileContent.append(line).append(System.lineSeparator());
        }
        // TODO find better solution for files with new lines - not to append line separators
        fileContent.setLength(fileContent.length() - 1);
      } else {
        LOG.error("Unable to read file from provided path: " + path);
        System.exit(2);
      }
    } catch (IOException ex) {
      LOG.error("Cannot read content file \"" + path + "\"");
      ex.printStackTrace();
      System.exit(2);
    }
    return fileContent.toString();
  }
}
