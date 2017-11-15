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

package com.redhat.mqe;

import java.util.Map;

/**
 * Listener interface. It is intended to allow meaningful assertions about message content in tests
 * and possibly towards running the cli in single JVM in an OSGi container for faster startup.
 * <p>
 * The test usecase can be resolved by injecting a spying MessageFormatter. Still, that does not give
 * access to stdout. This would not be possible in OSGi usecase, because there we cannot set Dagger.
 * <p>
 * NOTE(jdanek): onOutput and onError are not called by the cli yet. Something to implement if
 * there is actual use for it and if it is not too disruptive.
 */
public interface ClientListener {
    /**
     * Accepts a newly arrived or sent message.
     * @param message Message that was sent or received
     */
    void onMessage(Map<String, Object> message);

    /**
     * Accepts line of output. Will not be called if it contains a message and onMessage was called.
     *
     * @param output One or more lines of stdout.
     */
    void onOutput(String output);

    /**
     * Accepts line of error output.
     * @param error One or more lines of stderr.
     */
    void onError(String error);
}
