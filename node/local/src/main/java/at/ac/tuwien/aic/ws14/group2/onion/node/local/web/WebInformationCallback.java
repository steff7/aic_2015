package at.ac.tuwien.aic.ws14.group2.onion.node.local.web;

import at.ac.tuwien.aic.ws14.group2.onion.directory.api.service.ChainNodeInformation;
import at.ac.tuwien.aic.ws14.group2.onion.node.local.node.ChainNodeMetaData;

import java.util.List;

/**
 * Created by Stefan on 25.01.15.
 */
public interface WebInformationCallback {

    void chainRequestResponse(long requestId, List<ChainNodeInformation> info);

    void chainBuildUpStep(long requestId, int stepNumber, ChainNodeMetaData node, boolean requestOrResponse, boolean success);

    void establishedTargetConnection(long requestId, TargetInfo info);

    void data(long requestId, byte[] data, boolean sentOrReceived);

    void error(long requestId, String errormsg);
}
