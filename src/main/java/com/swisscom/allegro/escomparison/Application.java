package com.swisscom.allegro.escomparison;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.swisscom.allegro.escomparison.options.BillSpecCompareTest;
import com.swisscom.allegro.escomparison.options.CustomerCompareTest;
import com.swisscom.allegro.escomparison.options.QpiCompareTest;
import com.swisscom.allegro.escomparison.options.SoiCompareTest;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.*;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.swisscom.allegro.escomparison.Config.*;


@Slf4j
@SpringBootApplication
public class Application {

    private static List<CompareTest> tests = new ArrayList<>();
    private static Scanner in = new Scanner(System.in);

    private static List<String> indexOrigItems = new ArrayList<>();
    private static List<String> indexNewItems = new ArrayList<>();

    private static List<Map<String, String>> differences = new ArrayList<>();

    private static TransportClient client;
    private static TransportClient compareClient;
    private static Integer i = 0;

    private static List<Importers> importers = new ArrayList<>();

    private static Boolean hasBeenChecked = false;

    private static void setupLocalElasticSearchClient() {
        log.debug("Connecting to local ES...");

        Settings settings = Settings.settingsBuilder()
                .put("cluster.name", LOCAL_CLUSTER_NAME)
                .build();

        client = TransportClient.builder().settings(settings).build()
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getLoopbackAddress(), 9300));

        compareClient = TransportClient.builder().settings(settings).build()
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getLoopbackAddress(), 9300));
    }

    public static void setupESCompareClient(String host, String clusterName) throws UnknownHostException {
        Application.log.debug("Connecting to {} ES...", host);

        Settings settings = Settings.settingsBuilder()
                .put("cluster.name", clusterName)
                .build();

        compareClient = TransportClient.builder().settings(settings).build()
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(host), 9300));
    }

    private static void setupElasticSearchClient(String host, String clusterName) throws UnknownHostException {
        Application.log.debug("Connecting to {} ES...", host);

        Settings settings = Settings.settingsBuilder()
                .put("cluster.name", clusterName)
                .build();

        client = TransportClient.builder().settings(settings).build()
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(host), 9300));
    }

    private static void importIndex(List<String> s, String indexName, String sortBy, String type) {
        importIndex(s, indexName, sortBy, type, Long.MAX_VALUE);
    }

    private static void importIndex(List<String> s, String indexName, String sortBy, String type, Long maxValues) {
        log.debug("Importing from {}...", indexName);

        i = 0;

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
                    getSoiFromLocalEnv(indexNewItems, "http://localhost:8090/inventory/import/CustomerOrderItem/", importers.get(importers.size() - 1).getItemNo());

                    if (index == maxValues)
                        break;
                }

                scrollResp = client.prepareSearchScroll(scrollResp.getScrollId()).setScroll(new TimeValue(5, TimeUnit.MINUTES)).execute().actionGet();
            } while(index < maxValues && scrollResp.getHits().getHits().length != 0);

            client.close();

        } else log.error("Cannot import, client is null!");
    }

    private static void perfromSearchAndCompareHit(SearchHit origHit) {

        hasBeenChecked = true;

        if (compareClient != null) {

            if (origHit.getSource().containsKey(QPI_SORT_BY)) {

                int soiNO = (int) origHit.getSource().get(QPI_SORT_BY);

                log.info("Importing soiNo: {}", soiNO);

                SearchResponse response = compareClient.prepareSearch(NEW_INDEX_NAME)
                        .setQuery(QueryBuilders.termsQuery(QPI_SORT_BY, String.valueOf(soiNO)))
                        .setSize(1)
                        .execute()
                        .actionGet();

                if (response.getHits().totalHits() > 0) {
                    indexNewItems.add(response.getHits().getAt(0).getSourceAsString());

                    String oldValue = origHit.getSourceAsString();
                    String newValue = response.getHits().getAt(0).getSourceAsString();

                    if (! areJsonsEqual(oldValue, newValue)) {

                        Map<String, String> diff = new HashMap<>();
                        diff.put(oldValue, newValue);

                        differences.add(diff);

                        log.info("Difference added!");
                    }
                } else {
                    hasBeenChecked = false;
                }
            }


        } else
            log.error("performSearchCompareHit: The client is null, please connect to ES!");
    }

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    private static String readJsonFromUrl(String url) throws IOException {
        try (InputStream is = new URL(url).openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            return readAll(rd);
        }
    }

    private static void getSoiFromLocalEnv(List<String> s, String url, Integer soiNo) {
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

    private static void removeJsonField(JsonElement json, String fieldName) {
        json.getAsJsonObject().remove(fieldName);
    }

    private static void handleExceptions(JsonElement el1, JsonElement el2) {
//        removeJsonField(el1, "soiChLab");
//        removeJsonField(el2, "soiChLab");
//
//        removeJsonField(el1, "soiOptLock");
//        removeJsonField(el2, "soiOptLock");
//
//        removeJsonField(el1, "soiChOrId");
//        removeJsonField(el2, "soiChOrId");
    }

    private static boolean areJsonsSemanticallyIdentical(String o, String n) {
        JsonParser parser = new JsonParser();
        JsonElement j1 = parser.parse(o);
        JsonElement j2 = parser.parse(n);

        handleExceptions(j1, j2);

        try {
            JSONCompareResult result = JSONCompare.compareJSON(new Gson().toJson(j1), new Gson().toJson(j2), JSONCompareMode.LENIENT);
            return !result.failed();
        } catch (JSONException e) {
            log.error("Json compare failed: {}", e.getMessage());
        }

        return false;
    }

    private static boolean areJsonsEqual(String oldVersion, String newVersion) {
        boolean status = oldVersion.equals(newVersion);
        if (areJsonsSemanticallyIdentical(oldVersion, newVersion)) {
            if (!status && SHOW_NON_SEMANTIC_WARNINGS) {
                log.warn("The json objects are semantically identical but there was a difference found!");
                log.debug("Old version: {}", oldVersion);
                log.debug("New version: {}", newVersion);
            }

            return true;
        }

        log.error("The json objects are different!");
        log.debug("Old version: {}", oldVersion);
        log.debug("New version: {}", newVersion);

        return false;
    }

    private static void setupLocal() {
        setupLocalElasticSearchClient();
    }

    private static void setupRemote() {
        try {
            setupElasticSearchClient(REMOTE_HOST, REMOTE_CLUSTER_NAME);
        } catch (UnknownHostException e) {
            log.error("Failed to connect!\n{}", e.getMessage());
        }
    }

    private static void importAndCompareCustomers() {
        importIndex(indexOrigItems, "index_pbtaifun", CUSTOMER_SORT_BY, IMPORT_TYPE_CUSTOMER);
        importIndex(indexNewItems, "index_new_pbtaifun", CUSTOMER_SORT_BY, IMPORT_TYPE_CUSTOMER);
    }

    private static void importAndCompareSOIs() {//
//        importIndex(indexOrigItems, OLD_IAAS_INDEX_NAME, SOI_SORT_BY, IMPORT_TYPE_SOI);
        importIndex(indexOrigItems, OLD_IAAS_INDEX_NAME, INVENTORY_SORT_BY, IMPORT_TYPE_INVENTORY, 10L);
//        importIndex(indexNewItems, "index_new_pbtaifun", SOI_SORT_BY, IMPORT_TYPE_SOI, 100L);
    }

    private static void compare() {
        if (indexNewItems.size() != indexOrigItems.size()) {
            log.error("The indexes are not matching. Old index contains {} elements and the new one contains {}.", indexOrigItems.size(), indexNewItems.size());
        } else {
            List<String> itemsLeft = indexOrigItems.stream()
                    .filter(document -> !areJsonsEqual(document, indexNewItems.get(indexOrigItems.indexOf(document))))
                    .collect(Collectors.toList());

            if (itemsLeft.isEmpty() && differences.isEmpty())
                Application.log.debug("The indexes are a match! Old: {} New: {}", indexOrigItems.size(), indexNewItems.size());
            else
                Application.log.error("You have differences between the two indexes. {} changes were found.", itemsLeft.size());
        }
    }

    private static void runTests() {

        if (USE_LOCAL_ES) {
//            importIndex(indexOrigItems, OLD_INDEX_NAME, BILL_SPEC_SORT_BY, IMPORT_TYPE_BILL_SPEC);
            importIndex(indexOrigItems, OLD_INDEX_NAME, INVENTORY_SORT_BY, IMPORT_TYPE_INVENTORY);
//            importIndex(indexNewItems, NEW_INDEX_NAME, BILL_SPEC_SORT_BY, IMPORT_TYPE_BILL_SPEC);
        } else {
//            importAndCompareCustomers();
            importAndCompareSOIs();
        }

        log.debug("Starting comparison...");

        if (!hasBeenChecked)
            compare();
        else {

            if (differences.isEmpty())
                log.info("No difference has been found!");

            else {
                log.info("Showing differences:");

                differences.forEach(diff -> log.error(diff.toString()));
            }
        }
    }

    private static double getTimeFromMsInSec(double time) {
        return time / 1_000_000_000.0;
    }

    public static void main(String[] args) {
//
//        initOptions();
//        showWelcomeMessage();
//
//        Optional<CompareTest> test;
//
//        do {
//            showOptions();
//            test = getTest(readOption());
//            in.nextLine();
//            test.ifPresent(CompareTest::exec);
//        } while (test.isPresent());

        if (USE_LOCAL_ES)
            setupLocal();
        else {
            setupRemote();

            try {
                setupESCompareClient(REMOTE_HOST, REMOTE_CLUSTER_NAME);
            } catch (UnknownHostException e) {
                log.error("Could not connect to remote ES!: {}", e.getMessage());
            }
        }

        long startTime = System.nanoTime();

        runTests();

        long endTime = System.nanoTime();
        log.debug("Comparison took: {} sec", getTimeFromMsInSec((endTime - startTime)));
    }

    private static void showWelcomeMessage() {
        System.out.println("Welcome to Importer Tester V2.0");
        System.out.println("Note: To exit please select an invalid test.");
    }

    private static void initOptions() {
        tests.add(new CustomerCompareTest());
        tests.add(new SoiCompareTest());
        tests.add(new BillSpecCompareTest());
        tests.add(new QpiCompareTest());
    }

    private static void showOptions() {
        System.out.println("\nPlease select a test to run:");
        tests.forEach(option -> {
            int index = tests.indexOf(option) + 1;
            System.out.println(index + ". " + option.name);
        });
    }

    private static Optional<CompareTest> getTest(int no) {
        if (no < 0 || no > tests.size() - 1)
            return Optional.empty();

        return Optional.of(tests.get(no));
    }

    private static int readOption() {
        System.out.print("> ");
        return in.nextInt() - 1;
    }
}
