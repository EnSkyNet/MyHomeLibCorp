package com.myhomelibcorp.domain.model;

import java.util.Objects;

/**
 * Модель автора книги.
 * Містить ім'я, по батькові, прізвище та унікальний ідентифікатор.
 */
public final class Author {
    private final long id;
    private final String firstName;
    private final String middleName;
    private final String lastName;

    /**
     * Конструктор для створення автора з окремими частинами ПІБ.
     * @param id унікальний ідентифікатор
     * @param firstName ім'я
     * @param middleName по батькові
     * @param lastName прізвище
     */
    public Author(long id, String firstName, String middleName, String lastName) {
        this.id = id;
        this.firstName = firstName != null ? firstName.trim() : "";
        this.middleName = middleName != null ? middleName.trim() : "";
        this.lastName = lastName != null ? lastName.trim() : "Невідомий Автор";
    }

    /**
     * Конструктор для створення автора з повного імені (прізвище та ініціали).
     * @param id унікальний ідентифікатор
     * @param fullName повне ім'я у форматі "Прізвище Ім'я По-батькові"
     */
    public Author(long id, String fullName) {
        this.id = id;
        String[] parts = fullName.split(" ", 3);
        if (parts.length >= 1) this.lastName = parts[0];
        else this.lastName = "Невідомий Автор";
        if (parts.length >= 2) this.firstName = parts[1];
        else this.firstName = "";
        if (parts.length >= 3) this.middleName = parts[2];
        else this.middleName = "";
    }

    public long id() { return id; }
    public String firstName() { return firstName; }
    public String middleName() { return middleName; }
    public String lastName() { return lastName; }

    /**
     * Повертає ПІБ у форматі "Прізвище Ім'я По-батькові".
     * @return рядок з ПІБ
     */
    public String displayFullName() {
        StringBuilder sb = new StringBuilder(lastName);
        if (!firstName.isEmpty()) sb.append(" ").append(firstName);
        if (!middleName.isEmpty()) sb.append(" ").append(middleName);
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Author author = (Author) o;
        return Objects.equals(displayFullName().toLowerCase(), author.displayFullName().toLowerCase());
    }

    @Override
    public int hashCode() {
        return Objects.hash(displayFullName().toLowerCase());
    }

    @Override
    public String toString() {
        return displayFullName();
    }
}