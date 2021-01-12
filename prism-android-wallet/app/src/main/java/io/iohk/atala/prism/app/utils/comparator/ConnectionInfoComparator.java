package io.iohk.atala.prism.app.utils.comparator;

import java.util.Comparator;

import io.iohk.atala.prism.protos.ConnectionInfo;

/**
 * Compare the ConnectionInfo object according to the issuer name
 * */

public class ConnectionInfoComparator implements Comparator<ConnectionInfo> {

    public int compare(ConnectionInfo o1, ConnectionInfo o2) {
        return o1.getParticipantInfo().getIssuer().getName().compareTo(
                o2.getParticipantInfo().getIssuer().getName());
    }
}
