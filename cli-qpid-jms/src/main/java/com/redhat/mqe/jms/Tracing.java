/*
 * Copyright (c) 2019 Red Hat, Inc.
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

import io.jaegertracing.Configuration;
import io.jaegertracing.Configuration.ReporterConfiguration;
import io.jaegertracing.Configuration.SamplerConfiguration;
import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.opentracing.util.GlobalTracer;

public final class Tracing {
    private Tracing() {
    }

    public static JaegerTracer init(String service) {
        SamplerConfiguration samplerConfig = SamplerConfiguration.fromEnv()
            .withType(ConstSampler.TYPE)
            .withParam(1);

        ReporterConfiguration reporterConfig = ReporterConfiguration.fromEnv()
            .withLogSpans(true);

        Configuration config = Configuration.fromEnv(service)
            .withSampler(samplerConfig)
            .withReporter(reporterConfig);

        return config.getTracer();
    }

    public static JaegerTracer register(String service) {
        JaegerTracer tracer = Tracing.init(service);
        GlobalTracer.register(tracer);
        return tracer;
    }
}
