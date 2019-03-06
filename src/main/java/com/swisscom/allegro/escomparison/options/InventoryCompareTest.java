package com.swisscom.allegro.escomparison.options;

import com.swisscom.allegro.escomparison.CompareTest;
import com.swisscom.allegro.escomparison.ImporterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.swisscom.allegro.escomparison.Config.*;

@Component
public class InventoryCompareTest extends CompareTest {

    @Autowired
    ImporterService importerService;

    public InventoryCompareTest() {
        this.name = "Inventory Importer Test";
    }

    @Override
    public void exec() {
        importerService.importIndex(importerService.indexOrigItems, OLD_IAAS_INDEX_NAME, INVENTORY_SORT_BY, IMPORT_TYPE_INVENTORY, 10L);
    }
}
