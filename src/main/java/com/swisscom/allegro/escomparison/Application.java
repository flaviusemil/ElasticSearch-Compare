package com.swisscom.allegro.escomparison;

import com.google.gson.*;
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
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.PostConstruct;
import java.io.*;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

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

	private final Config config;

	Application(Config config) {
		this.config = config;
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

		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		final int availableProcessors = Runtime.getRuntime().availableProcessors();
		executor.setMaxPoolSize(Math.max(availableProcessors * 2, 16));
		executor.setQueueCapacity(availableProcessors * 2);
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		executor.initialize();

		hasBeenChecked = true;
		Application.i = 0;
		AtomicLong counter = new AtomicLong(0);
		List<String> failedCompareResults = new ArrayList<>();
		if (client != null) {
			SearchResponse scrollResp = client.prepareSearch(indexName)
					.setQuery(QueryBuilders.termQuery("_type", type))
					.addSort(sortBy, SortOrder.ASC)
//                    .setQuery(QueryBuilders.termQuery("itemNo", 1826123))
//                    .setQuery(QueryBuilders.termQuery("_id", 12913240))
					.setFrom(6500000)
					.setScroll(new TimeValue(5, TimeUnit.MINUTES))
					.setSize(100)
					.execute()
					.actionGet();

			long index = 0;

			do {
				for (SearchHit hit : scrollResp.getHits().getHits()) {
					index++;
					counter.incrementAndGet();
//                    if (counter.get() > 300000) {
					compareInThread(executor, failedCompareResults, hit.getSourceAsString(), counter.get());
//                    }

					if (counter.get() == maxValues)
						break;
				}

				scrollResp = client.prepareSearchScroll(scrollResp.getScrollId()).setScroll(new TimeValue(5, TimeUnit.MINUTES)).execute().actionGet();
			} while (index < maxValues && scrollResp.getHits().getHits().length != 0);

			client.close();

		} else log.error("Cannot import, client is null!");

		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.shutdown();

		log.error("Failed: ");
		failedCompareResults.forEach(System.out::println);
	}

	private static void compareInThread(ThreadPoolTaskExecutor executorService, List<String> failedCompareResults, String sourceAsString, long count) {
		executorService.execute(() -> {

			final Importers row = new Gson().fromJson(sourceAsString, Importers.class);

			final int prodNo = row.getProdNo();
			try {
//                    if (row.getProdNo() != 1715019)
//                        continue;

				log.info("Importing item no: {}, id: {}", count, prodNo);
				String newJson = getFromLocalEnv("http://localhost:8090/inventory/import/Product/", prodNo);

				boolean result = areJsonsSemanticallyIdentical(sourceAsString, newJson);
				if (result) {
					log.info("The json objects are semantically identical for item no (count): " + count + "; for prod no: " + prodNo);
				} else {
					failedCompareResults.add(String.valueOf(prodNo));
				}

				log.error("Failed: " + String.join("; ", failedCompareResults));
			} catch (Exception e) {
				log.error("Compare failed for item no (count): " + count + "; for prod no: " + prodNo, e);
			}

		});
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

	private static String getFromLocalEnv(String url, Integer soiNo) {
		try {
			String json = readJsonFromUrl(url + soiNo);

			if (json == null)
				return null;

			if (json.substring(0, 1).equals("["))
				return json.substring(1, json.length() - 1);
			else
				return json;

		} catch (IOException e) {
			log.error("Invalid url: {}", e.getMessage());
		}

		return null;
	}

	private static void removeJsonField(JsonElement json, String fieldName) {
		json.getAsJsonObject().remove(fieldName);
	}

	private static void removeJsonFieldCharacs(JsonElement json) {
		final JsonObject jsonObject = json.getAsJsonObject();
		jsonObject.remove("characs");
		JsonArray array = jsonObject.getAsJsonArray("child");
		array.forEach(el -> removeJsonFieldCharacs(el.getAsJsonObject()));
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

//        removeJsonField(el1, "child");
//        removeJsonField(el2, "child");

//        removeJsonFieldCharacs(el1);
//        removeJsonFieldCharacs(el2);

	}

	private static boolean areJsonsSemanticallyIdentical(String o, String n) {
		JsonParser parser = new JsonParser();
		JsonElement j1 = parser.parse(o);
		JsonElement j2 = parser.parse(n);

		handleExceptions(j1, j2);

		try {
			final String expectedStr = new Gson().toJson(j1);
			final String actualStr = new Gson().toJson(j2);
			JSONCompareResult result = JSONCompare.compareJSON(expectedStr, actualStr, JSONCompareMode.LENIENT);
			if (result.failed()) {
				log.error("getFieldMissing");
				result.getFieldMissing().forEach(f -> log.error(f.getField() + " getExpected: " + f.getExpected() + " getActual:" + f.getActual()));
				log.error("getFieldUnexpected");
				result.getFieldUnexpected().forEach(f -> log.error(f.getField() + " getExpected: " + f.getExpected() + " getActual:" + f.getActual()));
				log.error("getFieldFailures");
				result.getFieldFailures().forEach(f -> log.error(f.getField() + " getExpected: " + f.getExpected() + " getActual:" + f.getActual()));

				log.error("#The json objects are different: ");
				log.debug("#Old version: {}", expectedStr);
				log.debug("#New version: {}", actualStr);
				log.debug("================================");

			}
			return !result.failed();
		} catch (JSONException e) {
			log.error("Json compare failed: {}", e.getMessage());
		}

		return false;
	}

	private static double getTimeFromMsInSec(double time) {
		return time / 1_000_000_000.0;
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
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

	private void setupLocalElasticSearchClient() {
		log.debug("Connecting to local ES...");

		Settings settings = Settings.settingsBuilder()
				.put("cluster.name", config.localClusterName)
				.build();

		client = TransportClient.builder().settings(settings).build()
				.addTransportAddress(new InetSocketTransportAddress(InetAddress.getLoopbackAddress(), 9300));

		compareClient = TransportClient.builder().settings(settings).build()
				.addTransportAddress(new InetSocketTransportAddress(InetAddress.getLoopbackAddress(), 9300));
	}

	private void perfromSearchAndCompareHit(SearchHit origHit) {

		hasBeenChecked = true;

		if (compareClient != null) {

			if (origHit.getSource().containsKey(config.qpiSortBy)) {

				int soiNO = (int) origHit.getSource().get(config.qpiSortBy);

				log.info("Importing soiNo: {}", soiNO);

				SearchResponse response = compareClient.prepareSearch(config.localNewIndexName)
						.setQuery(QueryBuilders.termsQuery(config.qpiSortBy, String.valueOf(soiNO)))
						.setSize(1)
						.execute()
						.actionGet();

				if (response.getHits().totalHits() > 0) {
					indexNewItems.add(response.getHits().getAt(0).getSourceAsString());

					String oldValue = origHit.getSourceAsString();
					String newValue = response.getHits().getAt(0).getSourceAsString();

					if (areJsonsEqual(oldValue, newValue)) {

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

	private boolean areJsonsEqual(String oldVersion, String newVersion) {
		boolean areJsonsSemanticallyIdentical = areJsonsSemanticallyIdentical(oldVersion, newVersion);

		if (areJsonsSemanticallyIdentical && config.showNonSemanticWarnings && oldVersion.equals(newVersion)) {
			log.warn("The json objects are semantically identical but there was a difference found!");
			log.debug("Old version: {}", oldVersion);
			log.debug("New version: {}", newVersion);
		}

		return areJsonsSemanticallyIdentical;
	}

	private void setupLocal() {
		setupLocalElasticSearchClient();
	}

	private void setupRemote() {
		try {
			setupElasticSearchClient(config.remoteHost, config.remoteClusterName);
		} catch (UnknownHostException e) {
			log.error("Failed to connect!\n{}", e.getMessage());
		}
	}

	private void importAndCompareCustomers() {
		importIndex(indexOrigItems, "index_pbtaifun", config.customerSortBy, config.importTypeCustomer);
		importIndex(indexNewItems, "index_new_pbtaifun", config.customerSortBy, config.importTypeCustomer);
	}

	private void importAndCompareSOIs() {//
//        importIndex(indexOrigItems, remoteOldIndexName, config.soiSortBy, config.importTypeSoi);
		importIndex(indexOrigItems, config.remoteOldIndexName, config.inventorySortBy, config.importTypeInventory, config.MAX_VALUES_TO_COMPARE);
//        importIndex(indexNewItems, "index_new_pbtaifun", config.soiSortBy, config.importTypeSoi, 100L);
	}

	private void compare() {
		if (indexNewItems.size() != indexOrigItems.size()) {
			log.error("The indexes are not matching. Old index contains {} elements and the new one contains {}.", indexOrigItems.size(), indexNewItems.size());
		} else {
			List<String> itemsLeft = indexOrigItems.stream()
					.filter(document -> areJsonsEqual(document, indexNewItems.get(indexOrigItems.indexOf(document))))
					.collect(Collectors.toList());

			if (itemsLeft.isEmpty() && differences.isEmpty())
				Application.log.debug("The indexes are a match! Old: {} New: {}", indexOrigItems.size(), indexNewItems.size());
			else
				Application.log.error("You have differences between the two indexes. {} changes were found.", itemsLeft.size());
		}
	}

	private void runTests() {

		if (config.useLocalES) {
//            importIndex(indexOrigItems, localOldIndexName, config.billSpecSortBy, config.importTypeBillSpec);
			importIndex(indexOrigItems, config.localOldIndexName, config.inventorySortBy, config.importTypeInventory);
//            importIndex(indexNewItems, localNewIndexName, config.billSpecSortBy, config.importTypeBillSpec);
		} else {
//            importAndCompareCustomers();
			System.out.println("Remote cluster: " + config.remoteClusterName);
			System.out.println("Remote host: " + config.remoteHost);

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

	@PostConstruct
	private void init() {
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

		if (config.useLocalES)
			setupLocal();
		else {
			setupRemote();

			try {
				setupESCompareClient(config.remoteHost, config.remoteClusterName);
			} catch (UnknownHostException e) {
				log.error("Could not connect to remoteClusterName ES!: {}", e.getMessage());
			}
		}

		long startTime = System.nanoTime();

		runTests();

		long endTime = System.nanoTime();
		log.debug("Comparison took: {} sec", getTimeFromMsInSec((endTime - startTime)));
	}
}
