package com.myhomelibcorp.domain.service;

import com.myhomelibcorp.domain.model.Fb2Book;
import java.util.List;

public record ImportResult(List<Fb2Book> scannedBooks, int savedBooks) {}