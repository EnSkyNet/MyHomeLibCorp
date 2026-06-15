package com.myhomelibcorp.domain.service;

import com.myhomelibcorp.domain.model.Book;
import com.myhomelibcorp.domain.model.SearchCriteria;
import com.myhomelibcorp.infrastructure.database.sqlite.SearchRepository;
import java.util.List;

public class SearchService {
    private final SearchRepository searchRepository;
    public SearchService(SearchRepository searchRepository) { this.searchRepository = searchRepository; }
    public List<Book> findBooks(SearchCriteria criteria) {
        if (criteria == null) return List.of();
        String cleanTitle = criteria.title() != null ? criteria.title().trim() : "";
        String cleanAuthor = criteria.author() != null ? criteria.author().trim() : "";
        String cleanSeries = criteria.series() != null ? criteria.series().trim() : "";
        String cleanLang = "Всі мови".equals(criteria.language()) ? "" : criteria.language();
        SearchCriteria optimized = new SearchCriteria(cleanTitle, cleanAuthor, "", cleanSeries, cleanLang);
        return searchRepository.searchBooks(optimized);
    }
    public List<String> getAvailableLanguages() { return searchRepository.getDistinctLanguages(); }
}