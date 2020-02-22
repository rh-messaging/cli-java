package com.redhat.amqx.main;

import org.apache.log4j.PropertyConfigurator;

import java.util.Properties;

/**
 * Utility class to configure log messages
 */
public class LogConfigurator {
    /**
     * Restricted constructor
     */
    private LogConfigurator() {
    }


    private static void configureCommon(Properties properties) {
        properties.setProperty("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
        properties.setProperty("log4j.appender.stdout.Target", "System.out");
        properties.setProperty("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout");
        properties.setProperty("log4j.appender.stdout.layout.ConversionPattern", "%m%n");
    }

    private static void configureTrace(Properties properties) {
        properties.setProperty("log4j.rootLogger", "TRACE, stdout");
    }

    private static void configureDebug(Properties properties) {
        properties.setProperty("log4j.rootLogger", "DEBUG, stdout");
    }

    private static void configureInfo(Properties properties) {
        properties.setProperty("log4j.rootLogger", "INFO, stdout");
    }

    private static void configureError(Properties properties) {
        properties.setProperty("log4j.rootLogger", "ERROR, stdout");
    }


    /**
     * Configure the output to be at trace level
     */
    public static void trace() {
        Properties properties = new Properties();

        configureCommon(properties);
        configureTrace(properties);

        PropertyConfigurator.configure(properties);
    }

    /**
     * Configure the output to be at debug level
     */
    public static void debug() {
        Properties properties = new Properties();

        configureCommon(properties);
        configureDebug(properties);
        PropertyConfigurator.configure(properties);
    }

    /**
     * Configure the output to be at info (info) level
     */
    public static void info() {
        Properties properties = new Properties();

        configureCommon(properties);
        configureInfo(properties);
        PropertyConfigurator.configure(properties);
    }

    /**
     * Configure the output to be as error as possible
     */
    public static void error() {
        Properties properties = new Properties();

        configureCommon(properties);
        configureError(properties);

        PropertyConfigurator.configure(properties);
    }
}
