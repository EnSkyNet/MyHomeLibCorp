package com.myhomelibcorp.domain.service;

import com.myhomelibcorp.domain.model.Book;
import com.myhomelibcorp.domain.model.SearchCriteria;
import com.myhomelibcorp.infrastructure.database.sqlite.SearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Сервіс пошуку книг за критеріями.
 * Використовує SearchRepository для виконання пошукових запитів.
 */
public class SearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);
    private final SearchRepository searchRepository;

    public SearchService(SearchRepository searchRepository) {
        this.searchRepository = searchRepository;
    }

    /**
     * Пошук книг за критеріями.
     * @param criteria об'єкт з параметрами пошуку
     * @return список знайдених книг
     */
    public List<Book> findBooks(SearchCriteria criteria) {
        if (criteria == null) {
            return Collections.emptyList();
        }

        // Очищення та підготовка критеріїв
        String cleanTitle = criteria.title() != null ? criteria.title().trim() : "";
        String cleanAuthor = criteria.author() != null ? criteria.author().trim() : "";
        String cleanSeries = criteria.series() != null ? criteria.series().trim() : "";
        String cleanLang = "Всі мови".equals(criteria.language()) ? "" : criteria.language();

        SearchCriteria optimizedCriteria = new SearchCriteria(
                cleanTitle, cleanAuthor, "", cleanSeries, cleanLang
        );

        logger.debug("Пошук за критеріями: title={}, author={}, series={}, lang={}",
                cleanTitle, cleanAuthor, cleanSeries, cleanLang);

        return searchRepository.searchBooks(optimizedCriteria);
    }

    /**
     * Повертає список всіх мов, які зустрічаються в бібліотеці.
     */
    public List<String> getAvailableLanguages() {
        return searchRepository.getDistinctLanguages();
    }
}