# Arguments for DEV's (comment static FROM and uncomnnet #DEV ones)
ARG UBI_VERSION=9
ARG OPENJDK_VERSION=17
ARG UBI_BUILD_TAG=latest
ARG UBI_RUNTIME_TAG=latest
ARG IMAGE_BUILD=registry.access.redhat.com/ubi${UBI_VERSION}/openjdk-${OPENJDK_VERSION}:${UBI_BUILD_TAG}
ARG IMAGE_BASE=registry.access.redhat.com/ubi${UBI_VERSION}/openjdk-${OPENJDK_VERSION}-runtime:${UBI_RUNTIME_TAG}

#DEV FROM $IMAGE_BUILD AS build
FROM ${IMAGE_BUILD} AS build

USER root
COPY . /app
WORKDIR /app

RUN microdnf -y --setopt=install_weak_deps=0 --setopt=tsflags=nodocs install \
    jq \
    && microdnf clean all -y

ENV MAVEN_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Dmaven.repo.local=/app/.m2 -Dmaven.artifact.threads=42"
RUN mvn clean && mvn -T 1C package -DskipTests=true --no-transfer-progress

RUN mkdir targets && \
    cp cli-qpid-jms/target/cli-qpid-jms-*[0-9].jar targets/cli-qpid.jar && \
    cp cli-artemis-jms/target/cli-artemis-jms-*[0-9].jar targets/cli-artemis.jar && \
    cp cli-paho-java/target/cli-paho-java-*[0-9].jar targets/cli-paho.jar && \
    cp cli-activemq/target/cli-activemq-*[0-9].jar targets/cli-activemq.jar && \
    cp cli-protonj2/target/cli-protonj2-*[0-9].jar targets/cli-protonj2.jar && \
    ls -1 cli-*/target/cli-*.jar > VERSION.txt

WORKDIR /tmp
RUN mkdir hivemq-mqtt && \
    cd hivemq-mqtt && \
    curl -sLO $(curl -s https://api.github.com/repos/hivemq/mqtt-cli/releases/latest | jq -r '.assets[] | select(.name | endswith(".jar")) | .browser_download_url') && \
    cp mqtt-cli-*[0-9].jar /app/targets/cli-hivemq-mqtt.jar && \
    ls -1 mqtt-cli-*[0-9].jar | sed -e 's/mqtt-cli/cli-hivemq-mqtt/'>> /app/VERSION.txt

WORKDIR /app

#DEV FROM $IMAGE_BASE
FROM ${IMAGE_BASE}

LABEL name="Red Hat Messaging QE - Java CLI Image" \
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
