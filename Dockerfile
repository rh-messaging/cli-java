# Arguments for DEV's (comment static FROM and uncomnnet #DEV ones)
ARG UBI_VERSION=9
ARG OPENJDK_VERSION=17
ARG UBI_BUILD_TAG=latest
ARG UBI_RUNTIME_TAG=latest
ARG IMAGE_BUILD=registry.access.redhat.com/ubi${UBI_VERSION}/openjdk-${OPENJDK_VERSION}:${UBI_TAG}
ARG IMAGE_BASE=registry.access.redhat.com/ubi${UBI_VERSION}/openjdk-${OPENJDK_VERSION}-runtime:${UBI_RUNTIME_TAG}

#DEV FROM $IMAGE_BUILD AS build
FROM registry.access.redhat.com/ubi9/openjdk-17:1.15-1.1686736679 AS build

USER root
COPY . /app
WORKDIR /app

ENV MAVEN_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Dmaven.repo.local=/app/.m2 -Dmaven.artifact.threads=42"
RUN mvn -T 1C package -DskipTests=true --no-transfer-progress

RUN mkdir targets && \
    cp cli-qpid-jms/target/cli-qpid-jms-*[0-9].jar targets/cli-qpid.jar && \
    cp cli-artemis-jms/target/cli-artemis-jms-*[0-9].jar targets/cli-artemis.jar && \
    cp cli-paho-java/target/cli-paho-java-*[0-9].jar targets/cli-paho.jar && \
    cp cli-activemq/target/cli-activemq-*[0-9].jar targets/cli-activemq.jar && \
    cp cli-protonj2/target/cli-protonj2-*[0-9].jar targets/cli-protonj2.jar && \
    echo "package info:("$(ls cli-*/target/cli-*.jar)")" >> VERSION.txt

#DEV FROM $IMAGE_BASE
FROM registry.access.redhat.com/ubi9/openjdk-17-runtime:1.15-1

LABEL name="Red Hat Messagign QE - Java CLI Image" \
      run="podman run --rm -ti <image_name:tag> /bin/bash cli-*"

USER root

# install fallocate for use by claire tests
RUN microdnf -y --setopt=install_weak_deps=0 --setopt=tsflags=nodocs install \
    util-linux \
    && microdnf clean all -y

RUN mkdir /licenses 
COPY ./LICENSE /licenses/LICENSE.txt
COPY ./image/bin /usr/local/bin
COPY --from=build /app/targets/ /opt/cli-java
COPY --from=build /app/VERSION.txt /opt/cli-java

RUN chmod 0755 /usr/local/bin/cli-* && \
    chmod +x /usr/local/bin/cli-*

RUN mkdir /var/lib/cli-java && \
    chown -R 1001:0 /var/lib/cli-java  && \
    chmod -R g=u /var/lib/cli-java

USER 1001

VOLUME /var/lib/cli-java
WORKDIR /var/lib/cli-java

CMD ["/bin/bash"]
