package com.redhat.amqx.main;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

import java.util.function.Consumer;

/**
 * Utility class to configure log messages
 * <p>
 * https://logging.apache.org/log4j/2.x/manual/api.html
 * https://logging.apache.org/log4j/2.x/manual/customconfig.html
 */
public class LogConfigurator {
    /**
     * Restricted constructor
     */
    private LogConfigurator() {
    }


    private static void configureCommon(Consumer<LoggerConfig> customizeConfig) {
        final LoggerContext context = LoggerContext.getContext(false);
        final Configuration config = context.getConfiguration();
        LoggerConfig rootLogger = config.getRootLogger();

        customizeConfig.accept(rootLogger);

        context.updateLoggers();
    }

    public static void trace() {
        configureCommon((LoggerConfig config) -> config.setLevel(Level.TRACE));
    }

    /**
     * Configure the output to be at debug level
     */
    public static void debug() {
        configureCommon((LoggerConfig config) -> config.setLevel(Level.DEBUG));
    }

    /**
     * Configure the output to be at info (info) level
     */
    public static void info() {
        configureCommon((LoggerConfig config) -> config.setLevel(Level.INFO));
    }

    /**
     * Configure the output to be as error as possible
     */
    public static void error() {
        configureCommon((LoggerConfig config) -> config.setLevel(Level.ERROR));
    }
}
