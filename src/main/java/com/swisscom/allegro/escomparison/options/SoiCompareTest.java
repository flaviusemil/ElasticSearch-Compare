package com.swisscom.allegro.escomparison.options;

import com.google.gson.JsonElement;
import com.swisscom.allegro.escomparison.CompareTest;
import org.springframework.stereotype.Component;

@Component
public class SoiCompareTest extends CompareTest {

    public SoiCompareTest() {
        this.name = "SOI Importer Test";
    }

    @Override
    public void exec() {

    }

    @Override
    protected void handleExceptions(JsonElement el1, JsonElement el2) {

    }
}
