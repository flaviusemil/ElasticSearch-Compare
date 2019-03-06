package com.swisscom.allegro.escomparison.client;

import org.elasticsearch.client.transport.TransportClient;
import org.springframework.stereotype.Service;

import java.net.UnknownHostException;

@Service
public interface ClientService {

    TransportClient getLocalClient();
    TransportClient getLocalClient(String clusterName);
    TransportClient getRemoteClient(String host, String clusterName) throws UnknownHostException;

}
