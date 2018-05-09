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

import com.redhat.mqe.lib.*;
import dagger.Binds;
import dagger.BindsInstance;
import dagger.Component;
import dagger.Module;

import javax.inject.Named;

@Module(includes = AacClientModule.Declarations.class)
final class AacClientModule {
    @Module
    interface Declarations {
        @Binds
        ConnectionManagerFactory bindConnectionManagerFactory(AacConnectionManagerFactory f);

        @Binds
        JmsMessageFormatter bindMessageFormatter(AacAMQPJmsMessageFormatter f);

        @Binds
        ClientOptionManager bindClientOptionManager(AacClientOptionManager m);

        @Binds
        @Named("Sender")
        ClientOptions bindClientOptions(AacSenderOptions o);

        @Binds
        @Named("Receiver")
        ClientOptions bindReceiverOptions(AacReceiverOptions o);

        @Binds
        @Named("Connector")
        ClientOptions bindConnectorOptions(AacConnectorOptions o);
    }
}

@Component(modules = {
    AacClientModule.class
})
interface AacClient extends Client {
    @Component.Builder
    interface Builder {
        @BindsInstance
        Builder args(@Args String[] args);

        AacClient build();
    }
}

public class Main {
    public static void main(String[] args) throws Exception {
        AacClient client = DaggerAacClient.builder()
            .args(args).build();
        com.redhat.mqe.lib.Main.main(args, client);
    }
}
