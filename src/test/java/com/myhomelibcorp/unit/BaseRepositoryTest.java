package com.myhomelibcorp.unit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class BaseRepositoryTest {
    protected Connection connection;

    @BeforeEach
    void setUp() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement stmt = connection.createStatement()) {
            // Тут буде створення схеми (винести в окремий SQL-скрипт)
            stmt.execute("CREATE TABLE books (id INTEGER PRIMARY KEY, title TEXT)");
            // ... решта таблиць (можна використати DatabaseManager.initializeDatabase())
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (connection != null) connection.close();
    }
}