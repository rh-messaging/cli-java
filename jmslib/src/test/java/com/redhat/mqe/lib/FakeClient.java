/*
 * Copyright (c) 2018 Red Hat, Inc.
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

import com.redhat.mqe.ClientListener;
import dagger.Binds;
import dagger.BindsInstance;
import dagger.Component;
import dagger.Module;
import org.jetbrains.annotations.Nullable;

import javax.inject.Named;


@Component(modules = {FakeClientModule.class})
interface FakeClient extends Client {
    ConnectionManagerFactory fakeConnectionManagerFactory();

    @Component.Builder
    abstract class Builder {
        @BindsInstance
        abstract Builder connectionManagerFactory(ConnectionManagerFactory f);

        @BindsInstance
        abstract Builder messageFormatter(JmsMessageFormatter f);

        @BindsInstance
        abstract Builder clientOptionManager(ClientOptionManager m);

        @BindsInstance
        abstract Builder args(@Args String[] args);

        @BindsInstance
        abstract Builder listener(@Nullable ClientListener listener);

        abstract FakeClient build();
    }
}

@Module(includes = FakeClientModule.Declarations.class)
class FakeClientModule {
    @Module
    interface Declarations {
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
