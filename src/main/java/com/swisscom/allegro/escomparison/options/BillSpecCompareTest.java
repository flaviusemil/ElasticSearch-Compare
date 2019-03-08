package com.swisscom.allegro.escomparison.options;

import com.google.gson.JsonElement;
import com.swisscom.allegro.escomparison.CompareTest;
import org.springframework.stereotype.Component;

@Component
public class BillSpecCompareTest extends CompareTest {

    public BillSpecCompareTest() {
        this.name = "Bill Spec Importer Test";
    }

    @Override
    public void exec() {

    }

    @Override
    protected void handleExceptions(JsonElement el1, JsonElement el2) {

    }
}
