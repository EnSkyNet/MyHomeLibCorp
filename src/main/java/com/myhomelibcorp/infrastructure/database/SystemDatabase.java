package com.myhomelibcorp.infrastructure.database;

import com.myhomelibcorp.domain.model.CollectionInfo;
import com.myhomelibcorp.domain.model.CollectionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class SystemDatabase implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(SystemDatabase.class);
    private final Connection connection;
    public SystemDatabase(Path path) throws SQLException {
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath());
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA foreign_keys=ON");
            st.execute("PRAGMA journal_mode=WAL");
            st.execute("""
                CREATE TABLE IF NOT EXISTS Bases (
                    DatabaseID INTEGER PRIMARY KEY AUTOINCREMENT,
                    BaseName TEXT NOT NULL UNIQUE,
                    DBFileName TEXT NOT NULL,
                    RootFolder TEXT NOT NULL,
                    DataVersion INTEGER,
                    Code INTEGER NOT NULL,
                    LibUser TEXT, LibPassword TEXT, Notes TEXT,
                    CreationDate TEXT NOT NULL,
                    URL TEXT, ConnectionScript TEXT
                )""");
            st.execute("CREATE TABLE IF NOT EXISTS Groups (GroupID INTEGER PRIMARY KEY, GroupName TEXT UNIQUE, AllowDelete INTEGER, Notes TEXT)");
            st.execute("CREATE TABLE IF NOT EXISTS BookGroups (BookID INTEGER, DatabaseID INTEGER, GroupID INTEGER, PRIMARY KEY(GroupID,BookID,DatabaseID))");
        }
    }
    public List<CollectionInfo> collections() throws SQLException {
        List<CollectionInfo> list = new ArrayList<>();
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM Bases")) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }
    private CollectionInfo map(ResultSet rs) throws SQLException {
        return new CollectionInfo(rs.getLong("DatabaseID"), rs.getString("BaseName"),
                Path.of(rs.getString("DBFileName")), Path.of(rs.getString("RootFolder")),
                rs.getInt("DataVersion"), CollectionType.fromCode(rs.getInt("Code")),
                rs.getString("Notes"), rs.getString("LibUser"), rs.getString("LibPassword"),
                rs.getString("URL"), rs.getString("ConnectionScript"),
                LocalDateTime.parse(rs.getString("CreationDate")));
    }
    @Override public void close() throws Exception { if (connection != null && !connection.isClosed()) connection.close(); }
}