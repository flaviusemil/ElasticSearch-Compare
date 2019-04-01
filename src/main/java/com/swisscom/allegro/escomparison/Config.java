package com.swisscom.allegro.escomparison;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class Config {

    private Config() {}

    @Value("${application.useLocalES}")
    public boolean useLocalES;

    @Value("${application.showNonSemanticWarnings}")
    public boolean showNonSemanticWarnings;

    @Value("${application.port}")
    public int port;

    @Value("${application.maxValuesToCompare}")
    public long maxValuesToCompare;


    @Value("${local.elasticsearch.cluster.name}")
    public String localClusterName;

    @Value("${local.elasticsearch.cluster.oldIndexName}")
    public String localOldIndexName;

    @Value("${local.elasticsearch.cluster.newIndexName}")
    public String localNewIndexName;


    @Value("${remote.host}")
    public String remoteHost;

    @Value("${remote.elasticsearch.cluster.name}")
    public String remoteClusterName;

    @Value("${remote.elasticsearch.cluster.oldIndexName}")
    public String remoteOldIndexName;

    @Value("${remote.elasticsearch.cluster.newIndexName}")
    public String remoteNewIndexName;


    @Value("${importers.soi.type}")
    public String importTypeSoi;

    @Value("${importers.customer.type}")
    public String importTypeCustomer;

    @Value("${importers.qpi.type}")
    public String importTypeQpi;

    @Value("${importers.billSpec.type}")
    public String importTypeBillSpec;

    @Value("${importers.inventory.type}")
    public String importTypeInventory;

    @Value("${importers.soi.sortBy}")
    public String soiSortBy;

    @Value("${importers.customer.sortBy}")
    public String customerSortBy;

    @Value("${importers.qpi.sortBy}")
    public String qpiSortBy;

    @Value("${importers.billSpec.sortBy}")
    public String billSpecSortBy;

    @Value("${importers.inventory.sortBy}")
    public String inventorySortBy;
}
