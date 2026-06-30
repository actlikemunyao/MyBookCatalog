package com.example.mybookcatalog;

import java.io.Serializable;

public class Book implements Serializable {
    private String id;
    private String title;
    private String author;
    private String genre;
    private String description;
    private String imageUrl;
    private String section; // e.g., "featured", "popular", "new"

    // Required empty constructor for Firestore
    public Book() {}

    public Book(String title, String author, String genre, String description, String imageUrl) {
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.description = description;
        this.imageUrl = imageUrl;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
}
