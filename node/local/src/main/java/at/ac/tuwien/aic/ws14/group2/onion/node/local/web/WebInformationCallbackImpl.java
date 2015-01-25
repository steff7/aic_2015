package at.ac.tuwien.aic.ws14.group2.onion.node.local.web;

import at.ac.tuwien.aic.ws14.group2.onion.node.common.node.Endpoint;
import at.ac.tuwien.aic.ws14.group2.onion.node.local.node.ChainMetaData;
import at.ac.tuwien.aic.ws14.group2.onion.node.local.web.webInformation.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Stefan on 25.01.15.
 */
public class WebInformationCallbackImpl implements  WebInformationCallback {
    
    private ConcurrentHashMap<Long, ConcurrentLinkedQueue<RequestInfo>> requestInfoMap;
    
    public WebInformationCallbackImpl() {
        this.requestInfoMap = new ConcurrentHashMap<>();
    }

    @Override
    public void chainEstablished(long requestId, ChainMetaData chainMetaData) {
        ConcurrentLinkedQueue<RequestInfo> queue = new ConcurrentLinkedQueue<>();
        ChainEstablishedInfo chainRequestInfo = new ChainEstablishedInfo(chainMetaData);
        queue.add(chainRequestInfo);

        ConcurrentLinkedQueue<RequestInfo> oldInfo = requestInfoMap.putIfAbsent(requestId, queue);
        oldInfo.add(chainRequestInfo);
    }

    @Override
    public void chainBuildUp(long requestId, ChainMetaData chainMetaData) {
        ConcurrentLinkedQueue<RequestInfo> queue = new ConcurrentLinkedQueue<>();
        ChainBuildUpRequest chainBuildUpRequest = new ChainBuildUpRequest(chainMetaData);
        queue.add(chainBuildUpRequest);

        ConcurrentLinkedQueue<RequestInfo> oldInfo = requestInfoMap.putIfAbsent(requestId, queue);
        oldInfo.add(chainBuildUpRequest);
    }

    @Override
    public void establishedTargetConnection(long requestId, Endpoint endpoint) {
        ConcurrentLinkedQueue<RequestInfo> queue = new ConcurrentLinkedQueue<>();
        TargetRequestInfo targetRequestInfo = new TargetRequestInfo(endpoint);
        queue.add(targetRequestInfo);

        ConcurrentLinkedQueue<RequestInfo> oldInfo = requestInfoMap.putIfAbsent(requestId, queue);
        oldInfo.add(targetRequestInfo);
    }


    @Override
    public void dataSent(long requestId, byte[] data) {

    }

    @Override
    public void dataReceived(long requestId, byte[] data) {

    }

    @Override
    public void chainDestroyed(long requestId) {

    }

    @Override
    public void error(long requestId, String errormsg) {
        ConcurrentLinkedQueue<RequestInfo> queue = new ConcurrentLinkedQueue<>();
        ErrorRequestInfo errorRequestInfo = new ErrorRequestInfo(errormsg);
        queue.add(errorRequestInfo);

        ConcurrentLinkedQueue<RequestInfo> oldInfo = requestInfoMap.putIfAbsent(requestId, queue);
        oldInfo.add(errorRequestInfo);
    }

    @Override
    public void info(long requestId, String msg) {

    }
}
