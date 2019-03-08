package com.swisscom.allegro.escomparison.options;

import com.google.gson.JsonElement;
import com.swisscom.allegro.escomparison.CompareTest;
import com.swisscom.allegro.escomparison.importer.ImporterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.swisscom.allegro.escomparison.Config.*;

@Component
public class InventoryCompareTest extends CompareTest {

    @Autowired
    private ImporterService importerService;

    private List<String> indexOrigItems = new ArrayList<>();

    public InventoryCompareTest() {
        this.name = "Inventory Importer Test";
    }

    @Override
    public void exec() {
        importerService.importIndex(indexOrigItems, OLD_IAAS_INDEX_NAME, INVENTORY_SORT_BY, IMPORT_TYPE_INVENTORY, 10L);

    }

    @Override
    protected void handleExceptions(JsonElement el1, JsonElement el2) {

    }
}
