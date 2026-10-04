package com.library.interfaces;
import java.util.List;
public interface Searchable {
    List<?> searchByTitle(String title);
    List<?> searchByAuthor(String author);
    List<?> searchByISBN(String isbn);
}
