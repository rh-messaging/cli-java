package com.redhat.amqx.management.artemis;

import com.redhat.amqx.main.NodeType;
import com.redhat.amqx.management.Credentials;
import com.redhat.amqx.management.exception.DestinationException;
import org.apache.activemq.artemis.api.core.management.AddressControl;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Destination object management for Apache Artemis/AMQ7 broker.
 */
public class DivertArtemisManager extends AbstractArtemisManager {

    public DivertArtemisManager(String url, Credentials credentials, String brokerName, String host) throws IOException {
        super(url, credentials, brokerName, host);
    }

    List<String> getDiverts() throws Exception {
        return Arrays.asList(getServerControlMBean().getDivertNames());
    }

    protected boolean divertExists(String name) throws Exception {
        if (name == null) {
            throw new IllegalArgumentException("Divert name not supplied!");
        }
        return getDiverts().contains(name);
    }


    public void listDiverts(boolean isVerbose) throws Exception {
        String reportJson;
        List<String> divertNames = getDiverts();
        // TODO
        if (isVerbose) {
            JSONObject jsonObject = new JSONObject();
            for (String divertName : divertNames) {
                String address = getDivertAddress(divertName);
                if (address == null) {
                    continue;  // See bug in getDivertAddress()
                }
                jsonObject.put(divertName, getDestinationProperties(address, divertName, NodeType.DIVERT));
            }
            reportJson = jsonObject.toString();
        } else {
            reportJson = new JSONArray(divertNames).toString();
        }
        formatter.printConvertedJson(reportJson);
    }


    public String getDivertAddress(String divertName) throws Exception {
        for (String addr : getAddresses()) {
            AddressControl ac = (AddressControl) getResolver().getAddressView(addr);
            List<String> addrBindingNames = Arrays.asList(ac.getBindingNames());
                if (addrBindingNames.contains(divertName)) {
                    return addr;
                }
        }
        // ENTMQBR-1051 Diverts created using mbeans createDivert() create weird addresses
        return null;
    }

    public boolean destinationExists(String name) throws Exception {
        if (name == null) {
            throw new IllegalArgumentException("Divert name not supplied!");
        }
        return getDiverts().contains(name);
    }

    public void removeDivert(String destinationName, String addressName) throws Exception {
        if (divertExists(destinationName)) {
            getServerControlMBean().destroyDivert(destinationName);
            logger.info(String.format("Divert '%s' removed", destinationName));
        } else {
            throw new DestinationException("Divert '" + destinationName + "' does not exist!");
        }
    }


    public void addDivert(String divertName, String routingName, String addressName, String forwardingAddress,
                          boolean exclusive, String selector, String routingType) throws Exception {
        if (addressName == null) {
            addressName = divertName;
        }
        if (divertExists(divertName)) {
            throw new DestinationException("Divert '" + divertName + "' already exists!");
        } else {
            String transformerClass = null;
            getServerControlMBean().createDivert(divertName, routingName, addressName, forwardingAddress, exclusive, selector, transformerClass, routingType);
            logger.info(String.format("Divert '%s' created", divertName));
        }
    }

    public void getDestinationProperties(String addressName, String destinationName) throws Exception {
        if (addressName == null) {
            addressName = destinationName;
        }
        if (divertExists(destinationName)) {
            formatter.printConvertedJson(new JSONObject(getDestinationProperties(addressName, destinationName, NodeType.DIVERT)).toString());
        } else {
            throw new DestinationException(String.format("Divert '%s' does not exist!", destinationName));
        }
    }

}
