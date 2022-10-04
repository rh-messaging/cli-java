FROM fedora:latest

WORKDIR /var/lib/cli-java

ENV LANG=C.UTF-8
RUN yum install \
    -y --setopt=install_weak_deps=0 --setopt=tsflags=nodocs \
    java-11-openjdk-headless \
    bzip2 unzip xz \
    bzr git mercurial openssh-clients subversion procps \
    gnupg dirmngr \
    ca-certificates curl wget \
    && dnf clean all -y

RUN mkdir /main

COPY cli-qpid-jms/target/cli-qpid-jms-1.2.2-SNAPSHOT-*.jar /main/cli-qpid.jar
COPY cli-activemq/target/cli-activemq-1.2.2-SNAPSHOT-*.jar /main/cli-activemq.jar
COPY cli-artemis-jms/target/cli-artemis-jms-1.2.2-SNAPSHOT-*.jar /main/cli-artemis.jar
COPY cli-paho-java/target/cli-paho-java-1.2.2-SNAPSHOT-*.jar /main/cli-paho.jar

COPY create_links.sh /main
RUN bash /main/create_links.sh


RUN groupadd cli-java && useradd -d /var/lib/cli-java -ms /bin/bash -g cli-java -G cli-java cli-java
USER cli-java:cli-java
