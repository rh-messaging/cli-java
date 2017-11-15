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

package com.redhat.mqe.aoc;

import com.redhat.mqe.ClientListener;
import com.redhat.mqe.lib.Client;
import com.redhat.mqe.lib.*;
import dagger.Binds;
import dagger.BindsInstance;
import dagger.Component;
import dagger.Module;
import org.jetbrains.annotations.Nullable;

import javax.inject.Named;

@Module(includes = AocClientModule.Declarations.class)
final class AocClientModule {
    @Module
    interface Declarations {
        @Binds
        ConnectionManagerFactory bindConnectionManagerFactory(AocConnectionManagerFactory f);

        @Binds
        JmsMessageFormatter bindMessageFormatter(OpenwireJmsMessageFormatter f);

        @Binds
        ClientOptionManager bindClientOptionManager(AocClientOptionManager m);

        @Binds
        @Named("Sender")
        ClientOptions bindClientOptions(SenderOptions o);

        @Binds
        @Named("Receiver")
        ClientOptions bindReceiverOptions(ReceiverOptions o);

        @Binds
        @Named("Connector")
        ClientOptions bindConnectorOptions(ConnectorOptions o);
    }
}

@Component(modules = {
    AocClientModule.class
})
interface AocClient extends Client {
    @Component.Builder
    interface Builder {
        @BindsInstance
        Builder args(@Args String[] args);

        @BindsInstance
        Builder listener(@Nullable ClientListener listener);

        AocClient build();
    }
}

public class Main {
    public static void main(ClientListener listener, String[] args) throws Exception {
        AocClient client = DaggerAocClient.builder()
            .args(args).listener(listener).build();
        com.redhat.mqe.lib.Main.main(args, client);
    }

    public static void main(String[] args) throws Exception {
        main(null, args);
    }
}
