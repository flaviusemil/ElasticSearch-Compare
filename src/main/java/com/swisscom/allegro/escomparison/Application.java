package com.swisscom.allegro.escomparison;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.swisscom.allegro.escomparison.options.*;
import lombok.RequiredArgsConstructor;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;
import java.io.*;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.swisscom.allegro.escomparison.Config.*;
import static com.swisscom.allegro.escomparison.ImporterService.*;
import static java.util.logging.Level.INFO;


@Slf4j
@SpringBootApplication
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class Application {

    private static List<CompareTest> tests = new ArrayList<>();
    private static Scanner in = new Scanner(System.in);

    private static List<Map<String, String>> differences = new ArrayList<>();

    private static TransportClient client;
    private static TransportClient compareClient;
    private static Integer i = 0;

    private static List<Importers> importers = new ArrayList<>();

    private static Boolean hasBeenChecked = false;

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
//                    indexNewItems.add(response.getHits().getAt(0).getSourceAsString());

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
//        setupLocalElasticSearchClient();
    }

    private static void setupRemote() {
//        try {
//            setupElasticSearchClient(REMOTE_HOST, REMOTE_CLUSTER_NAME);
//        } catch (UnknownHostException e) {
//            log.error("Failed to connect!\n{}", e.getMessage());
//        }
    }

    private final CustomerCompareTest customerCompareTest;
    private final SoiCompareTest soiCompareTest;
    private final BillSpecCompareTest billSpecCompareTest;
    private final QpiCompareTest qpiCompareTest;
    private final InventoryCompareTest inventoryCompareTest;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);

        if (USE_LOCAL_ES)
            setupLocal();
        else {
            setupRemote();

//            try {
//                setupESCompareClient(REMOTE_HOST, REMOTE_CLUSTER_NAME);
//            } catch (UnknownHostException e) {
//                log.error("Could not connect to remote ES!: {}", e.getMessage());
//            }
        }
    }

    @PostConstruct
    private void init() {
        initOptions();
        showWelcomeMessage();

        Optional<CompareTest> test;

        do {
            showOptions();
            test = getTest(readOption());
            in.nextLine();
            long startTime = System.nanoTime();

            test.ifPresent(CompareTest::exec);

            long endTime = System.nanoTime();
            log.debug("Comparison took: {} sec", getTimeFromMsInSec((endTime - startTime)));
        } while (test.isPresent());
    }

    private static double getTimeFromMsInSec(double time) {
        return time / 1_000_000_000.0;
    }

    private static void showWelcomeMessage() {
        System.out.println("\nWelcome to Importer Tester V2.0");
        System.out.println("Note: To exit please select an invalid test.");
    }

    private void initOptions() {
        tests.add(customerCompareTest);
        tests.add(soiCompareTest);
        tests.add(billSpecCompareTest);
        tests.add(qpiCompareTest);
        tests.add(inventoryCompareTest);
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
