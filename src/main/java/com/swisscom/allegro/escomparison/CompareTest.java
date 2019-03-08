package com.swisscom.allegro.escomparison;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

import static com.swisscom.allegro.escomparison.Config.*;
import static com.swisscom.allegro.escomparison.Config.QPI_SORT_BY;

@Slf4j
public abstract class CompareTest {

    protected String name;

    public abstract void exec();

//    private static void perfromSearchAndCompareHit(SearchHit origHit) {
//
//        hasBeenChecked = true;
//
//        if (compareClient != null) {
//
//            if (origHit.getSource().containsKey(QPI_SORT_BY)) {
//
//                int soiNO = (int) origHit.getSource().get(QPI_SORT_BY);
//
//                log.info("Importing soiNo: {}", soiNO);
//
//                SearchResponse response = compareClient.prepareSearch(NEW_INDEX_NAME)
//                        .setQuery(QueryBuilders.termsQuery(QPI_SORT_BY, String.valueOf(soiNO)))
//                        .setSize(1)
//                        .execute()
//                        .actionGet();
//
//                if (response.getHits().totalHits() > 0) {
//                    indexNewItems.add(response.getHits().getAt(0).getSourceAsString());
//
//                    String oldValue = origHit.getSourceAsString();
//                    String newValue = response.getHits().getAt(0).getSourceAsString();
//
//                    if (! areJsonsEqual(oldValue, newValue)) {
//
//                        Map<String, String> diff = new HashMap<>();
//                        diff.put(oldValue, newValue);

//                        differences.add(diff);

//                        log.info("Difference added!");
//                    }
//                } else {
//                    hasBeenChecked = false;
//                }
//            }
//
//
//        } else
//            log.error("performSearchCompareHit: The client is null, please connect to ES!");
//    }

    protected boolean areJsonsEqual(String oldVersion, String newVersion) {
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

    private boolean areJsonsSemanticallyIdentical(String o, String n) {
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

    public void removeJsonField(JsonElement json, String fieldName) {
        json.getAsJsonObject().remove(fieldName);
    }

    protected abstract void handleExceptions(JsonElement el1, JsonElement el2);
}
