#!/usr/bin/env bash

# from examples/features/standard/ssl-enabled-dual-authentication/readme.html

keytool -genkey -keystore server-side-keystore.jks -storepass secureexample -keypass secureexample -dname "CN=ActiveMQ Artemis Server, OU=Artemis, O=ActiveMQ, L=AMQ, S=AMQ, C=AMQ" -keyalg RSA
keytool -export -keystore server-side-keystore.jks -file server-side-cert.cer -storepass secureexample

# client certificates
#keytool -import -keystore client-side-truststore.jks -file server-side-cert.cer -storepass secureexample -keypass secureexample -noprompt
#keytool -genkey -keystore client-side-keystore.jks -storepass secureexample -keypass secureexample -dname "CN=ActiveMQ Artemis Client, OU=Artemis, O=ActiveMQ, L=AMQ, S=AMQ, C=AMQ" -keyalg RSA
#keytool -export -keystore client-side-keystore.jks -file client-side-cert.cer -storepass secureexample
#keytool -import -keystore server-side-truststore.jks -file client-side-cert.cer -storepass secureexample -keypass secureexample -noprompt
