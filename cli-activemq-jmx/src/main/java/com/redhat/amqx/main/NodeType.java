package com.redhat.amqx.main;

/**
 * Enumeration for destination types. Topics and Queues only supported right now.
 */
public enum NodeType {
    TOPIC("Topic"),
    ADDRESS("Address"),
    QUEUE("Queue"),
    DIVERT("Divert");

    private String nodeTypeString;

    NodeType(final String nodeTypeString) {
        this.nodeTypeString = nodeTypeString;
    }

    public String getNodeTypeAsString() {
        return nodeTypeString;
    }

    public String getNodeTypeAsStringPlural() {
        return nodeTypeString + "s";
    }
}
