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

package com.redhat.mqe;

import com.google.common.truth.Truth;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import util.Broker;
import util.BrokerFixture;

import java.security.Permission;
import java.util.concurrent.TimeUnit;

class SystemExitingWithStatus extends RuntimeException {
    public final int status;

    public SystemExitingWithStatus(int status) {
        this.status = status;
    }
}

class NoExitSecurityManager extends SecurityManager {
    private final SecurityManager parentManager;

    @Override
    public void checkPermission(Permission perm) {
        // allow all
    }

    @Override
    public void checkPermission(Permission perm, Object context) {
        // allow all
    }

    @Override
    public void checkExit(int status) {
        throw new SystemExitingWithStatus(status);
    }

    NoExitSecurityManager(SecurityManager parentManager) {
        this.parentManager = parentManager;
    }

    public static void assertSystemExit(int status, @NotNull Executable executable) throws Throwable {
        Intrinsics.checkNotNullParameter(executable, "executable");
        SecurityManager previousManager = System.getSecurityManager();

        try {
            NoExitSecurityManager manager = new NoExitSecurityManager(previousManager);
            System.setSecurityManager(manager);
            executable.execute();
            Assertions.fail("expected exception");
        } catch (SystemExitingWithStatus exception) {
            Truth.assertThat(exception.status).isEqualTo(status);
        } finally {
            System.setSecurityManager(previousManager);
        }
    }

    public static void assertNoSystemExit(@NotNull Function0 executable) {
        Intrinsics.checkNotNullParameter(executable, "executable");
        SecurityManager previousManager = System.getSecurityManager();

        try {
            NoExitSecurityManager manager = new NoExitSecurityManager(previousManager);
            System.setSecurityManager((SecurityManager) manager);
            executable.invoke();
        } catch (SystemExitingWithStatus var5) {
            Assertions.fail("System.exit has been called");
        } finally {
            System.setSecurityManager(previousManager);
        }
    }
}

class MainTest {
    @BeforeAll
    static void configureLogging() {
        // turn on extra verbose logging
        //Broker.configureLogging();
    }

    void checkMainInvocation(String cmd) throws Throwable {
        String[] args = cmd.split(" ");
        NoExitSecurityManager.assertSystemExit(0, () -> {
            Main.main(args);
        });
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    @ExtendWith(BrokerFixture.class)
    void testAckWithSessionClose(@BrokerFixture.TempBroker Broker broker) throws Throwable {
        broker.configuration.setSecurityEnabled(false);
        broker.configuration.setPersistenceEnabled(false); // this, or tmpdir, otherwise test runs interact
        broker.startBroker();

        // todo: have to handle amqp:// prefix
        String brokerUrl = "localhost:" + broker.addAMQPAcceptor();

        NoExitSecurityManager.assertSystemExit(0, () -> {
            Main.main(
                "sender", "--log-msgs=dict", "--broker=" + brokerUrl, "--conn-username=tckuser", "--conn-password=tckuser", "--address=test_default_username_right_password_right", "--count=1"
            );
        });

        NoExitSecurityManager.assertSystemExit(0, () -> {
            Main.main(
                "sender", "--timeout=2", "--log-msgs=dict", "--broker=" + brokerUrl, "--conn-auth-mechanisms=PLAIN", "--conn-username=admin", "--conn-password=admin", "--address=test_direct_transient_empty_message_with_string_property", "--count=10", "--msg-property=key1=value1"
            );
        });

        NoExitSecurityManager.assertSystemExit(0, () -> {
            Main.main(
                "receiver", "--timeout=2", "--log-msgs=dict", "--broker=" + brokerUrl, "--conn-auth-mechanisms=PLAIN", "--conn-username=admin", "--conn-password=admin", "--address=test_direct_transient_empty_message_with_string_property", "--count=10"
            );
        });

        NoExitSecurityManager.assertSystemExit(0, () -> {
            Main.main(
                "sender", "--log-msgs=dict", "--broker=" + brokerUrl, "--conn-auth-mechanisms=PLAIN", "--conn-username=admin", "--conn-password=admin", "--address=test_direct_transient_text_message", "--count=1", "--msg-content=Simple Text Message", "--msg-correlation-id=corr-id-JWXoIk"
            );
        });

        // test_direct_transient_text_message
        checkMainInvocation("sender --log-msgs dict --broker " + brokerUrl + " --conn-auth-mechanisms PLAIN --conn-username admin --conn-password admin --address test_direct_transient_text_message --count 1 --msg-content SimpleTextMessage --msg-correlation-id corr-id-eqa9vp");
        checkMainInvocation("receiver --log-msgs dict --broker " + brokerUrl + " --conn-auth-mechanisms PLAIN --conn-username admin --conn-password admin --address test_direct_transient_text_message --count 1");

        // test_publish_subscribe_string
        Thread t = new Thread(() -> {
            try {
                checkMainInvocation("receiver --timeout 100 --log-msgs dict --broker " + brokerUrl + " --conn-auth-mechanisms PLAIN --conn-username admin --conn-password admin --address topic://test_publish_subscribe_string --count 3");
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        });
        Thread.sleep(100);  // do I really want to do things like this? need better check I have a subscriber on broker
        checkMainInvocation("sender --log-msgs dict --broker " + brokerUrl + " --conn-auth-mechanisms PLAIN --conn-username admin --conn-password admin --address topic://test_publish_subscribe_string --count 3 --msg-content ABC --msg-correlation-id some-corr-id");
        t.join();
    }

    @Test
    @Disabled("These commands take way too long to execute, now that --duration works how it should")
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    @ExtendWith(BrokerFixture.class)
    void test2(@BrokerFixture.TempBroker Broker broker) throws Throwable {
        broker.configuration.setSecurityEnabled(false);
        broker.configuration.setPersistenceEnabled(false); // this, or tmpdir, otherwise test runs interact
        broker.startBroker();

        // todo: have to handle amqp:// prefix
        String brokerUrl = "localhost:" + broker.addAMQPAcceptor();


        checkMainInvocation("receiver --timeout 2 --log-msgs dict --broker " + brokerUrl + " --conn-auth-mechanisms PLAIN --conn-username admin --conn-password admin --address JAMQMsgPatterns111Tests_test_browse_messages --recv-browse true --count 20");

        // tests.JAMQMsgPatterns000Tests.JAMQMsgPatternsTests.test_direct_transient_list_message
        checkMainInvocation("sender --log-msgs dict --broker " + brokerUrl + " --conn-auth-mechanisms PLAIN --conn-username admin --conn-password admin --address test_direct_transient_list_message --count 1 --msg-content-list-item  --msg-content-list-item String --msg-content-list-item ~1 --msg-content-list-item ~1.0 --msg-content-list-item 1 --msg-content-list-item 1.0 --msg-content-list-item ~-1 --msg-content-list-item ~-1.3 --msg-content-list-item -1 --msg-content-list-item ~~1 --msg-correlation-id corr-id-Mee2YQ");

        // tests.JAMQMsgPatterns000Tests.JAMQMsgPatternsTests.test_direct_transient_map_message
        checkMainInvocation("sender --log-msgs dict --broker " + brokerUrl + " --conn-auth-mechanisms PLAIN --conn-username admin --conn-password admin --address test_direct_transient_map_message --count 1 --msg-content-map-item empty_string= --msg-content-map-item string=String --msg-content-map-item int~1 --msg-content-map-item float~1.0 --msg-content-map-item string_int=1 --msg-content-map-item string_float=1.0 --msg-content-map-item negative_int~-1 --msg-content-map-item negative_float~-1.3 --msg-content-map-item string_negative_int=-1 --msg-content-map-item string_retype_operator=~1 --msg-correlation-id corr-id-JQt4JM");

        // tests.JAMQMsgPatterns000Tests.JAMQMsgPatternsTests.test_dead_letter_queue_with_expired_messages
        checkMainInvocation("sender --log-msgs dict --broker " + brokerUrl + " --conn-auth-mechanisms PLAIN --conn-username admin --conn-password admin --address test_dead_letter_queue_with_expired_messages --count 3 --msg-content ABC --msg-durable yes --msg-ttl 1000");

        // tests.JAMQMsgPatterns000Tests.JAMQMsgPatternsTests.test_message_group_simple
        checkMainInvocation("sender --log-msgs dict --broker " + brokerUrl + " --conn-auth-mechanisms PLAIN --conn-username admin --conn-password admin --address test_message_group_simple --count 1 --msg-content B-0 --msg-group-id B");

        // tests.JAMQMsgPatterns000Tests.JAMQMsgPatternsTests.test_reply_to_address
        checkMainInvocation("sender --log-msgs dict --broker " + brokerUrl + " --conn-auth-mechanisms PLAIN --conn-username admin --conn-password admin --address test_reply_to_address --count 1 --msg-reply-to test_reply_to_address-replyQ --msg-content-map-item text=replyQ_SLjFkenkBi");

        // tests.JAMQMsgPatterns000Tests.JAMQMsgPatternsTests.test_publish_subscribe_int
        checkMainInvocation("sender --log-msgs dict --broker " + brokerUrl + " --conn-auth-mechanisms PLAIN --conn-username admin --conn-password admin --address topic://test_publish_subscribe_int --count 3 --msg-content 12345 --content-type int --msg-correlation-id some-corr-id");

        // tests.JAMQMsgPatterns000Tests.JAMQMsgPatternsTests.test_publish_subscribe_map_list
        // --msg-content-map-item "key3~[123, 3.14]"

        // tests.JAMQMsgPatterns000Tests.JAMQMsgPatternsTests.test_message_group_consumer_disconnect
        // TODO msgcontent %d feature
        checkMainInvocation("sender --log-msgs dict --broker " + brokerUrl + " --conn-auth-mechanisms PLAIN --conn-username admin --conn-password admin --address test_message_group_consumer_disconnect --count 40 --msg-content A-%d --duration 20 --msg-group-id A");

        // tests.JAMQMsgPatterns000Tests.JAMQMsgPatternsTests.test_reply_to_address
        checkMainInvocation("receiver --timeout 5 --log-msgs dict --broker " + brokerUrl + " --conn-auth-mechanisms PLAIN --conn-username admin --conn-password admin --address test_reply_to_address --count 1 --process-reply-to");

        // tests.JAMQNode000Tests.JAMQNodeTests.test_address_full_policy_block
        checkMainInvocation("sender --log-msgs dict --broker " + brokerUrl + " --conn-auth-mechanisms PLAIN --conn-username admin --conn-password admin --address JAMQNode111Tests_test_address_full_policy_block --count 1 --msg-content-from-file /etc/passwd");

        // tests.JAMQNode000Tests.JAMQNodeTests.test_direct_transient_large_string_message_size_1mb
        // ditto for receiver
        checkMainInvocation("sender --log-msgs dict --broker " + brokerUrl + " --conn-auth-mechanisms PLAIN --conn-username admin --conn-password admin --address test_direct_transient_large_string_message_size_1mb --count 1 --msg-content-from-file /etc/passwd --msg-content-hashed True");

        // tests.JAMQNode000Tests.JAMQNodeTests.test_max_consumers_queue
        // TODO: client is not started at all in the test; some issues with params mapping?

        // tests.JAMQMessage000Tests.JAMQMessageTests.test_amqp_bare_message_consistency
        // Unknown options: '--msg-subject', 'amqp_bare_message_test', '--msg-user-id', 'admin', '--msg-priority', '7', '--conn-populate-user-id', 'True', '--msg-group-seq', '1', '--msg-reply-to-group-id', 'group-a'
        checkMainInvocation("sender --log-msgs dict --broker " + brokerUrl + " --conn-auth-mechanisms PLAIN --conn-username admin --conn-password admin --address test_amqp_bare_message_consistency --count 1 --msg-subject amqp_bare_message_test --msg-reply-to ExpiryQueue --msg-property PI=~3.141592 --msg-property color=red --msg-property mapKey=mapValue --msg-content amqp_bare_msg-CBJJIY --msg-durable True --msg-ttl 300000 --msg-correlation-id amqp_bare_msg-CBJJIY --msg-user-id admin --msg-priority 7 --conn-populate-user-id True --msg-group-id group-a --msg-group-seq 1 --msg-reply-to-group-id group-a");

        // tests.JAMQMessage000Tests.JAMQMessageTests.test_populate_validated_user_option
        // TODO FAIL     dtestlib.Test:levels.py:61 Checking properties keys for validated user with expected '_AMQ_VALIDATED_USER' or 'JMSXUserID': dict_keys([]) # result:False (exp. True), dur.:-1.00 err_cnt:1

        // tests.JAMQMessage000Tests.JAMQMessageTests.test_scheduled_message_zero_timestamp
        // Unknown options: '--msg-id'
        checkMainInvocation("sender --log-msgs dict --broker " + brokerUrl + " --conn-auth-mechanisms PLAIN --conn-username admin --conn-password admin --address JAMQMessage111Tests_test_scheduled_message_zero_timestamp --count 2 --msg-id ReferenceMessage_JAMQMessage111Tests_test_scheduled_message_zero_timestamp --msg-property _AMQ_SCHED_DELIVERY~0");

        // tests.JAMQMessage000Tests.JAMQMessageTests.test_client_acknowledge_inactivity_exception
        // --ssn-ack-mode
        // --duration --duration-mode --ssn-ack-mode
        checkMainInvocation("sender --log-msgs dict --broker " + brokerUrl + " --conn-auth-mechanisms PLAIN --conn-username admin --conn-password admin --address test_client_acknowledge_inactivity_exception --count 20 --msg-durable True --ssn-ack-mode client");
        checkMainInvocation("receiver --timeout 10 --log-msgs dict --broker " + brokerUrl + " --conn-auth-mechanisms PLAIN --conn-username admin --conn-password admin --address test_client_acknowledge_inactivity_exception --count 20 --duration 100 --duration-mode after-receive --ssn-ack-mode client");


//
//        checkMainInvocation("sender --log-msgs dict --broker " + brokerUrl + " --conn-auth-mechanisms PLAIN --conn-username admin --conn-password admin --address test_direct_transient_text_message --count 1 --msg-content SimpleTextMessage --msg-correlation-id corr-id-eqa9vp");
    }

//    void testMessageContentListItem() {
//        '--msg-content-list-item', '', '--msg-content-list-item', 'String', '--msg-content-list-item', '~1', '--msg-content-list-item', '~1.0', '--msg-content-list-item', '1', '--msg-content-list-item', '1.0', '--msg-content-list-item', '~-1', '--msg-content-list-item', '~-1.3', '--msg-content-list-item', '-1', '--msg-content-list-item', '~~1'
//    }
}
