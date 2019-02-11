package com.swisscom.allegro.escomparison;

public class Config {

    private Config() {}

    static final boolean USE_LOCAL_ES = false;
    static final boolean SHOW_NON_SEMANTIC_WARNINGS = false;

    static final String REMOTE_CLUSTER_NAME = "EL_Cluster_PBTAIFUN";
    static final String REMOTE_HOST = "taifun-be-h42-1";

    static final String LOCAL_CLUSTER_NAME = "EL_Cluster_DEVEL2";

    static final String OLD_INDEX_NAME = "index_al_doc01";
    static final String NEW_INDEX_NAME = "index_new_al_doc01";

    static final String OLD_IAAS_INDEX_NAME = "index_new_pbtaifun";

    static final String IMPORT_TYPE_SOI = "service_order_item";
    static final String IMPORT_TYPE_CUSTOMER = "customer";
    static final String IMPORT_TYPE_BILL_SPEC = "customer_bill_spec";
    static final String IMPORT_TYPE_QPI = "quoted_product";
    static final String IMPORT_TYPE_INVENTORY = "inventory";

    static final String CUSTOMER_SORT_BY = "custNo";
    static final String SOI_SORT_BY = "soiNo";
    static final String BILL_SPEC_SORT_BY = "cbsNo";
    static final String QPI_SORT_BY = "qpiNo";
    static final String INVENTORY_SORT_BY = "itemNo";
}
