package com.swisscom.allegro.escomparison.options;

import com.swisscom.allegro.escomparison.CompareTest;

public class CustomerCompareTest extends CompareTest {

    public CustomerCompareTest() {
        this.name = "Customer Importer Test";
    }

    @Override
    public void exec() {
        System.out.println("Testing customer importer...");
    }
}
