package com.swisscom.allegro.escomparison.importer;

import com.google.gson.Gson;
import com.swisscom.allegro.escomparison.Importers;
import com.swisscom.allegro.escomparison.client.ClientService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.swisscom.allegro.escomparison.Config.INVENTORY_IMPORT_API_URL;
import static com.swisscom.allegro.escomparison.Config.REMOTE_CLUSTER_NAME;

@Service
@Slf4j
class ImporterServiceImpl implements ImporterService {

    @Autowired
    private ClientService clientService;

    private TransportClient client;
    private static TransportClient compareClient;
    protected List<String> indexNewItems = new ArrayList<>();
    private List<Importers> importers = new ArrayList<>();

    @Override
    public void importIndex() {
        System.out.println("Running service...");
    }

    @Override
    public void importIndex(List<String> s, String indexName, String sortBy, String type) {
        client = clientService.getLocalClient(REMOTE_CLUSTER_NAME);
        importIndex(s, indexName, sortBy, type, Long.MAX_VALUE);
    }

    @Override
    public List<String> importIndex(List<String> s, String indexName, String sortBy, String type, Long maxValues) {

        if (client == null)
            client = clientService.getLocalClient(REMOTE_CLUSTER_NAME);

        log.debug("Importing from {}...", indexName);

        int i = 0;

        if (client != null) {
            SearchResponse scrollResp = client.prepareSearch(indexName)
                    .setQuery(QueryBuilders.termQuery("_type", type))
                    .addSort(sortBy, SortOrder.ASC)
//                    .setQuery(QueryBuilders.termQuery(sortBy, 2056))
                    .setScroll(new TimeValue(5, TimeUnit.MINUTES))
                    .setSize(100)
                    .execute()
                    .actionGet();

            long index = 0;

            do {
                for (SearchHit hit : scrollResp.getHits().getHits()) {
                    index++;

                    importers.add(new Gson().fromJson(hit.getSourceAsString(), Importers.class));
                    s.add(hit.getSourceAsString());
                    log.info("Importing item no: {}, id: {}", i++, importers.get(importers.size() - 1).getItemNo());
                    getSoiFromLocalEnv(indexNewItems, INVENTORY_IMPORT_API_URL, importers.get(importers.size() - 1).getItemNo());

                    if (index == maxValues)
                        break;
                }

                scrollResp = client.prepareSearchScroll(scrollResp.getScrollId()).setScroll(new TimeValue(5, TimeUnit.MINUTES)).execute().actionGet();
            } while(index < maxValues && scrollResp.getHits().getHits().length != 0);

            client.close();

        } else log.error("Cannot import, client is null!");
    }

    private void getSoiFromLocalEnv(List<String> s, String url, Integer soiNo) {
        String json = null;

        try {
            json = readJsonFromUrl(url + soiNo);
        } catch (IOException e) {
            log.error("Invalid url: {}", e.getMessage());
        }

        if (json != null)
            if (json.substring(0, 1).equals("["))
                s.add(json.substring(1, json.length() - 1));
            else s.add(json);
    }

    private String readJsonFromUrl(String url) throws IOException {
        try (InputStream is = new URL(url).openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            return readAll(rd);
        }
    }

    private String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }
}
