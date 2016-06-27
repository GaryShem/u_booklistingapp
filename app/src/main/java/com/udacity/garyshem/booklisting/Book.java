package com.udacity.garyshem.booklisting;

/**
 * A simple class to store book's author and title in a single object
 */
public class Book {
    private String author;
    private String title;

    @Override
    public String toString() {
        return "Book{" +
                "author='" + author + '\'' +
                ", title='" + title + '\'' +
                '}';
    }

    public Book(String author, String title) {
        this.author = author;
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public String getTitle() {
        return title;
    }
}
