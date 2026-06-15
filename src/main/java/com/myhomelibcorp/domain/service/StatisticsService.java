package com.myhomelibcorp.domain.service;

import com.myhomelibcorp.domain.model.SearchCriteria;
import com.myhomelibcorp.infrastructure.database.sqlite.SearchRepository;

public class StatisticsService {
    private final SearchRepository searchRepository;
    public StatisticsService(SearchRepository searchRepository) { this.searchRepository = searchRepository; }
    public long getTotalBooksCount() { return searchRepository.searchBooks(SearchCriteria.empty()).size(); }
    public String getDatabaseEngineInfo() { return "SQLite Core with FTS5 & WAL"; }
}