import org.apache.activemq.artemis.core.config.impl.SecurityConfiguration;
import org.apache.activemq.artemis.spi.core.security.ActiveMQJAASSecurityManager;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.LogManager;
import org.apache.log4j.SimpleLayout;
import org.apache.qpid.jms.exceptions.JMSSecuritySaslException;
import org.apache.qpid.jms.exceptions.JmsConnectionFailedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import util.Broker;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.JMSSecurityException;
import javax.jms.Session;

import static com.google.common.truth.Truth.assertThat;

@SuppressWarnings("Duplicates")
class QPIDJMS412Test {

    private static final String USER_NAME = "someUser";
    private static final String PASSWORD = "somePassword";

    @BeforeAll
    static void configureLogging() {
        ConsoleAppender consoleAppender = new ConsoleAppender(new SimpleLayout(), ConsoleAppender.SYSTEM_OUT);
        LogManager.getRootLogger().addAppender(consoleAppender);
    }

    @Test
    @Tag("issue")
    @DisplayName("client should stop reconnect if sasl auth fails on first server (auth is set up)")
    void reconnectOneServerSaslFailingConfigured() throws JMSException {
        try (Broker broker = new Broker()) {
            configureBroker(broker);

            broker.startBroker();

            ConnectionFactory f = new org.apache.qpid.jms.JmsConnectionFactory(
                "failover:(amqp://127.0.0.1:" + broker.addAMQPAcceptor() + ")");
            Assertions.assertThrows(JMSSecurityException.class, () -> {
                Connection c = f.createConnection(USER_NAME, "wrong_" + PASSWORD);
                c.start();
            }, "Expected connection creation to fail");
        }
    }

    @Test
    @Tag("issue")
    @DisplayName("client should stop reconnect if sasl auth fails on second server (auth is set up)")
    void reconnectTwoServersSaslFailingConfigured() throws JMSException {
        try (Broker broker = new Broker();
             Broker broker2 = new Broker()) {
            configureBroker(broker);

            broker.startBroker();
            broker2.startBroker();

            ConnectionFactory f = new org.apache.qpid.jms.JmsConnectionFactory(
                "failover:(amqp://127.0.0.1:" + broker.addAMQPAcceptor() + ",amqp://127.0.0.1:" + broker2.addAMQPAcceptor() + ")");
            Connection c = f.createConnection(USER_NAME, PASSWORD);
            c.start();

            broker.close();

            try {
                c.createSession(Session.CLIENT_ACKNOWLEDGE);
                Assertions.fail("Expected session creation to fail due to failed reconnect");
            } catch (JMSSecuritySaslException | JmsConnectionFailedException e) {
                assertThat(e).hasMessageThat().contains(
                    "Client failed to authenticate using SASL: PLAIN");
            }
        }
    }

    private void configureBroker(Broker broker) {
        SecurityConfiguration securityConfiguration = new SecurityConfiguration();
        securityConfiguration.addUser(USER_NAME, PASSWORD);
        ActiveMQJAASSecurityManager activeMQJAASSecurityManager = new ActiveMQJAASSecurityManager(
            "org.apache.activemq.artemis.spi.core.security.jaas.InVMLoginModule", securityConfiguration);
        broker.embeddedBroker.setSecurityManager(activeMQJAASSecurityManager);

        broker.configuration.setPersistenceEnabled(false);
        broker.configuration.setSecurityEnabled(true);
    }
}
