package com.swisscom.allegro.escomparison.client;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static com.swisscom.allegro.escomparison.Config.ELASTICSEARCH_PORT;
import static com.swisscom.allegro.escomparison.Config.LOCAL_CLUSTER_NAME;

@Service
@Slf4j
class ClientServiceImpl implements ClientService {

    private static final String CLUSTER_NAME_CONFIG = "cluster.name";

    @Override
    public TransportClient getLocalClient() {
        return getLocalClient(LOCAL_CLUSTER_NAME);
    }

    @Override
    public TransportClient getLocalClient(String clusterName) {
        log.info("Connecting to local ES...");

        Settings settings = Settings.settingsBuilder()
                .put(CLUSTER_NAME_CONFIG, clusterName)
                .build();

        return TransportClient.builder().settings(settings).build()
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getLoopbackAddress(), ELASTICSEARCH_PORT));
    }

    @Override
    public TransportClient getRemoteClient(String host, String clusterName) throws UnknownHostException {
        log.info("Connecting to {} ES...", host);

        Settings settings = Settings.settingsBuilder()
                .put(CLUSTER_NAME_CONFIG, clusterName)
                .build();

        return TransportClient.builder().settings(settings).build()
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(host), ELASTICSEARCH_PORT));
    }
}
