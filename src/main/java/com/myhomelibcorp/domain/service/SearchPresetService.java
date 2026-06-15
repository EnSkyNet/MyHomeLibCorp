package com.myhomelibcorp.domain.service;

import com.myhomelibcorp.domain.model.SearchCriteria;
import com.myhomelibcorp.domain.model.SearchPreset;
import com.myhomelibcorp.infrastructure.database.BookCollection;
import java.util.List;

public final class SearchPresetService {
    private final BookCollection database;
    public SearchPresetService(BookCollection database) { this.database = database; }
    public List<SearchPreset> getAllPresets() { return database.loadSearchPresets(); }
    public void createPreset(String name, SearchCriteria criteria) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Назва пресета не може бути порожньою");
        database.saveSearchPreset(name, criteria);
    }
    public void removePreset(long id) { database.deleteSearchPreset(id); }
}