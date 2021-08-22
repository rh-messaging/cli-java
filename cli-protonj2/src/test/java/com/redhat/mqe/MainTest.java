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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
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
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    @ExtendWith(BrokerFixture.class)
    void test2(@BrokerFixture.TempBroker Broker broker) throws Throwable {
        broker.configuration.setSecurityEnabled(false);
        broker.configuration.setPersistenceEnabled(false); // this, or tmpdir, otherwise test runs interact
        broker.startBroker();

        // todo: have to handle amqp:// prefix
        String brokerUrl = "localhost:" + broker.addAMQPAcceptor();


        checkMainInvocation("receiver --timeout 2 --log-msgs dict --broker " + brokerUrl + " --conn-auth-mechanisms PLAIN --conn-username admin --conn-password admin --address JAMQMsgPatterns111Tests_test_browse_messages --recv-browse true --count 20");

        checkMainInvocation("sender --log-msgs dict --broker " + brokerUrl + " --conn-auth-mechanisms PLAIN --conn-username admin --conn-password admin --address test_direct_transient_list_message --count 1 --msg-content-list-item  --msg-content-list-item String --msg-content-list-item ~1 --msg-content-list-item ~1.0 --msg-content-list-item 1 --msg-content-list-item 1.0 --msg-content-list-item ~-1 --msg-content-list-item ~-1.3 --msg-content-list-item -1 --msg-content-list-item ~~1 --msg-correlation-id corr-id-Mee2YQ");
        //checkMainInvocation("sender --log-msgs dict --broker " + brokerUrl + " --conn-auth-mechanisms PLAIN --conn-username admin --conn-password admin --address test_direct_transient_text_message --count 1 --msg-content SimpleTextMessage --msg-correlation-id corr-id-eqa9vp");
    }

    void testMessageContentListItem() {
//        '--msg-content-list-item', '', '--msg-content-list-item', 'String', '--msg-content-list-item', '~1', '--msg-content-list-item', '~1.0', '--msg-content-list-item', '1', '--msg-content-list-item', '1.0', '--msg-content-list-item', '~-1', '--msg-content-list-item', '~-1.3', '--msg-content-list-item', '-1', '--msg-content-list-item', '~~1'
    }
}
