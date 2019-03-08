package com.swisscom.allegro.escomparison.importer;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ImporterService {
    void importIndex();
    void importIndex(List<String> s, String indexName, String sortBy, String type);
    List<String> importIndex(List<String> s, String indexName, String sortBy, String type, Long maxValues);
}
