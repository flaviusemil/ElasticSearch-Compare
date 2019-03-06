package com.swisscom.allegro.escomparison.options;

import com.swisscom.allegro.escomparison.CompareTest;
import com.swisscom.allegro.escomparison.ImporterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CustomerCompareTest extends CompareTest {

    @Autowired
    ImporterService importerService;

    public CustomerCompareTest() {
        this.name = "Customer Importer Test";
    }

    @Override
    public void exec() {
        System.out.println("Testing customer importer...");
        importerService.importIndex();
    }
}
