package com.library.services;

import com.library.models.items.*;
import com.library.interfaces.*;
import com.library.exceptions.*;

import java.util.*;
import java.io.*;

/**
 * Chapter 1 (Complex Data Structures):
 *   - catalog changed from ArrayList to LinkedHashMap<String, LibraryItem>
 *     for O(1) lookup by itemId while preserving insertion order.
 *   - genres uses a HashSet<String> to automatically deduplicate genres.
 *   - mostBorrowedItems uses a LinkedHashMap<String, Integer> to track
 *     borrow counts per item in insertion order.
 *
 * Chapter 2 (Generics):
 *   - Extends Service<LibraryItem> and implements the generic findById().
 */
public class LibraryService extends Service<LibraryItem> implements Searchable {

    // Chapter 1: LinkedHashMap instead of ArrayList for O(1) itemId lookup
    private Map<String, LibraryItem> catalog;

    // Chapter 1: HashSet to collect unique genres (no duplicates)
    private Set<String> genres;

    // Chapter 1: LinkedHashMap to track borrow counts per itemId
    private Map<String, Integer> borrowCountMap;

    private int borrowCount = 0;

    public LibraryService() {
        this.catalog       = new LinkedHashMap<>();
        this.genres        = new HashSet<>();
        this.borrowCountMap = new LinkedHashMap<>();
        System.out.println("✓ LibraryService initialized.");
    }

    public boolean addItem(LibraryItem item) throws InvalidISBNException {
        if (item == null) {
            System.out.println("✗ Cannot add null item!");
            return false;
        }

        if (catalog.containsKey(item.getItemId())) {
            System.out.println("✗ Item with ID " + item.getItemId() + " already exists!");
            return false;
        }

        if (item instanceof Book) {
            Book book = (Book) item;
            if (!InvalidISBNException.isValidISBN(book.getISBN())) {
                throw new InvalidISBNException(book.getISBN(), "Invalid ISBN format. Must be 10 or 13 digits.");
            }
            // Chapter 1: add genre to the HashSet
            genres.add(book.getauthor()); // use author as genre-equivalent if no genre field
        }

        catalog.put(item.getItemId(), item);
        borrowCountMap.put(item.getItemId(), 0); // initialise borrow count
        System.out.println("✓ Item added: " + item.getTitle() + " (ID: " + item.getItemId() + ")");
        return true;
    }

    public boolean deleteItem(String itemId) throws ItemNotFoundException {
        LibraryItem item = catalog.get(itemId);

        if (item == null) {
            throw new ItemNotFoundException(itemId, "Item");
        }

        if (!item.isAvailable()) {
            System.out.println("⚠️  Item is currently borrowed. Cannot delete.");
            return false;
        }

        catalog.remove(itemId);
        borrowCountMap.remove(itemId);
        System.out.println("✓ Item deleted: " + item.getTitle());
        return true;
    }

    public boolean updateItem(String itemId, LibraryItem updatedItem) throws ItemNotFoundException, InvalidISBNException {
        if (!catalog.containsKey(itemId)) {
            throw new ItemNotFoundException(itemId, "Item");
        }

        if (updatedItem instanceof Book) {
            Book book = (Book) updatedItem;
            if (!InvalidISBNException.isValidISBN(book.getISBN())) {
                throw new InvalidISBNException(book.getISBN(), "Invalid ISBN format. Must be 10 or 13 digits.");
            }
        }

        catalog.put(itemId, updatedItem);
        System.out.println("✓ Item updated: " + updatedItem.getTitle());
        return true;
    }

    /** Chapter 2: implementation of generic findById from Service<LibraryItem> */
    @Override
    public LibraryItem findById(String itemId) {
        return catalog.get(itemId);
    }

    /** Original method kept for backward compatibility with LoanService / ReservationService */
    public LibraryItem findItemById(String itemId) {
        return catalog.get(itemId);
    }

    public LibraryItem findItemByIdWithException(String itemId) throws ItemNotFoundException {
        LibraryItem item = catalog.get(itemId);
        if (item == null) {
            throw new ItemNotFoundException(itemId, "Item");
        }
        return item;
    }

    public List<LibraryItem> getAllItems() {
        return new ArrayList<>(catalog.values());
    }

    /** Chapter 1: return the Set of unique genres/authors collected so far */
    public Set<String> getUniqueGenres() {
        return Collections.unmodifiableSet(genres);
    }

    /**
     * Chapter 1: increment borrow count for statistics / "most borrowed" feature.
     * Called by LoanService after a successful borrow.
     */
    public void incrementBorrowCount(String itemId) {
        borrowCountMap.put(itemId, borrowCountMap.getOrDefault(itemId, 0) + 1);
        borrowCount++;
    }

    /**
     * Chapter 1: return the top-N most borrowed items using the LinkedHashMap.
     */
    public List<LibraryItem> getMostBorrowedItems(int topN) {
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(borrowCountMap.entrySet());
        entries.sort((a, b) -> b.getValue() - a.getValue()); // descending

        List<LibraryItem> result = new ArrayList<>();
        for (int i = 0; i < Math.min(topN, entries.size()); i++) {
            LibraryItem item = catalog.get(entries.get(i).getKey());
            if (item != null) result.add(item);
        }
        return result;
    }

    // ──────────────────────────────────────────────────────────────
    // Searchable interface — unchanged logic
    // ──────────────────────────────────────────────────────────────

    @Override
    public List<LibraryItem> searchByTitle(String title) {
        List<LibraryItem> results = new ArrayList<>();

        if (title == null || title.trim().isEmpty()) {
            System.out.println("✗ Please enter a valid title to search.");
            return results;
        }

        String searchTitle = title.toLowerCase().trim();

        for (LibraryItem item : catalog.values()) {
            if (item.getTitle().toLowerCase().contains(searchTitle)) {
                results.add(item);
            }
        }

        if (results.isEmpty()) {
            System.out.println("✗ No items found matching: '" + title + "'");
        } else {
            System.out.println("✓ Found " + results.size() + " item(s) matching: '" + title + "'");
        }
        for (LibraryItem item : results) {
            item.displayInfo();
            System.out.println("--------------------------------------------------");
        }
        return results;
    }

    @Override
    public List<Book> searchByAuthor(String author) {
        List<Book> results = new ArrayList<>();

        if (author == null || author.trim().isEmpty()) {
            System.out.println("✗ Please enter a valid author name to search.");
            return results;
        }

        String searchAuthor = author.toLowerCase().trim();

        for (LibraryItem item : catalog.values()) {
            if (item instanceof Book) {
                Book book = (Book) item;
                if (book.getauthor().toLowerCase().contains(searchAuthor)) {
                    results.add(book);
                }
            }
        }

        if (results.isEmpty()) {
            System.out.println("✗ No books found by author: '" + author + "'");
        } else {
            System.out.println("✓ Found " + results.size() + " book(s) by author: '" + author + "'");
        }
        for (Book book : results) {
            book.displayInfo();
            System.out.println("--------------------------------------------------");
        }
        return results;
    }

    @Override
    public List<Book> searchByISBN(String isbn) {
        List<Book> results = new ArrayList<>();

        if (isbn == null || isbn.trim().isEmpty()) {
            System.out.println("✗ Please enter a valid ISBN to search.");
            return results;
        }

        String cleanISBN = isbn.replaceAll("[-\\s]", "").trim();

        for (LibraryItem item : catalog.values()) {
            if (item instanceof Book) {
                Book book = (Book) item;
                String bookISBN = book.getISBN().replaceAll("[-\\s]", "");
                if (bookISBN.equals(cleanISBN)) {
                    results.add(book);
                    break;
                }
            }
        }

        if (results.isEmpty()) {
            System.out.println("✗ No book found with ISBN: " + isbn);
        } else {
            System.out.println("✓ Book found with ISBN: " + isbn);
        }
        for (Book book : results) {
            book.displayInfo();
            System.out.println("--------------------------------------------------");
        }
        return results;
    }

    public List<LibraryItem> getAvailableItems() {
        List<LibraryItem> available = new ArrayList<>();
        for (LibraryItem item : catalog.values()) {
            if (item.isAvailable()) available.add(item);
        }
        return available;
    }

    public List<LibraryItem> getBorrowedItems() {
        List<LibraryItem> borrowed = new ArrayList<>();
        for (LibraryItem item : catalog.values()) {
            if (!item.isAvailable()) borrowed.add(item);
        }
        return borrowed;
    }

    // ──────────────────────────────────────────────────────────────
    // Service overrides — displayAll, displayStatistics, save, load, clear
    // ──────────────────────────────────────────────────────────────

    @Override
    public void displayAll() {
        if (catalog.isEmpty()) {
            System.out.println("\n📋 No items in the library catalog.\n");
            return;
        }

        System.out.println("\n=== LIBRARY CATALOG ===");
        System.out.println("Total Items: " + catalog.size());
        System.out.println("Unique Authors/Genres tracked: " + genres.size()); // Chapter 1

        for (LibraryItem item : catalog.values()) {
            item.displayInfo();
            System.out.println("----------------------------------------");
        }
    }

    @Override
    public void displayStatistics() {
        int books = 0, dvds = 0, magazines = 0, digitalBooks = 0;
        int available = 0, borrowed = 0;

        for (LibraryItem item : catalog.values()) {
            if (item instanceof DigitalBook)     digitalBooks++;
            else if (item instanceof Book)       books++;
            else if (item instanceof DVD)        dvds++;
            else if (item instanceof Magazine)   magazines++;

            if (item.isAvailable()) available++;
            else                    borrowed++;
        }

        System.out.println("\n=== LIBRARY STATISTICS ===");
        System.out.println("📚 Total Items:      " + catalog.size());
        System.out.println("📖 Books:            " + books);
        System.out.println("💻 Digital Books:    " + digitalBooks);
        System.out.println("📀 DVDs:             " + dvds);
        System.out.println("📰 Magazines:        " + magazines);
        System.out.println("✅ Available:        " + available);
        System.out.println("📤 Borrowed:         " + borrowed);
        System.out.println("🔤 Unique Authors:   " + genres.size()); // Chapter 1
        System.out.println("📊 Total Borrows:    " + borrowCount);   // Chapter 1

        // Chapter 1: show top-3 most borrowed
        List<LibraryItem> topItems = getMostBorrowedItems(3);
        if (!topItems.isEmpty()) {
            System.out.println("\n🏆 Top Borrowed Items:");
            for (int i = 0; i < topItems.size(); i++) {
                LibraryItem it = topItems.get(i);
                System.out.println("  " + (i + 1) + ". " + it.getTitle()
                        + " (" + borrowCountMap.getOrDefault(it.getItemId(), 0) + " borrows)");
            }
        }
    }

    @Override
    public boolean saveToFile(String filename) {
        File dataDir = new File("data");
        if (!dataDir.exists()) dataDir.mkdirs();

        String fullPath = "data/" + filename;

        try (PrintWriter writer = new PrintWriter(new FileWriter(fullPath))) {

            for (LibraryItem item : catalog.values()) {
                StringBuilder line = new StringBuilder();

                if (item instanceof DigitalBook) {
                    DigitalBook db = (DigitalBook) item;
                    line.append("DigitalBook|")
                        .append(item.getItemId()).append("|")
                        .append(item.getTitle()).append("|")
                        .append(db.getauthor()).append("|")
                        .append(db.getpublisher()).append("|")
                        .append(item.getYear()).append("|")
                        .append(db.getISBN()).append("|")
                        .append(db.getNp()).append("|")
                        .append(db.getFileSize()).append("|")
                        .append(db.getFormat()).append("|")
                        .append(db.getDownloadlink());
                } else if (item instanceof Book) {
                    Book book = (Book) item;
                    line.append("Book|")
                        .append(item.getItemId()).append("|")
                        .append(item.getTitle()).append("|")
                        .append(book.getauthor()).append("|")
                        .append(book.getpublisher()).append("|")
                        .append(item.getYear()).append("|")
                        .append(book.getISBN()).append("|")
                        .append(book.getNp()).append("|")
                        .append(item.isAvailable() ? "Available" : "Borrowed");
                } else if (item instanceof DVD) {
                    DVD dvd = (DVD) item;
                    line.append("DVD|")
                        .append(item.getItemId()).append("|")
                        .append(item.getTitle()).append("|")
                        .append(dvd.getDirector()).append("|")
                        .append(item.getYear()).append("|")
                        .append(dvd.getDuration()).append("|")
                        .append(dvd.getRating()).append("|")
                        .append(item.isAvailable() ? "Available" : "Borrowed");
                } else if (item instanceof Magazine) {
                    Magazine magazine = (Magazine) item;
                    line.append("Magazine|")
                        .append(item.getItemId()).append("|")
                        .append(item.getTitle()).append("|")
                        .append(magazine.getPublisher()).append("|")
                        .append(item.getYear()).append("|")
                        .append(magazine.getIsDate()).append("|")
                        .append(magazine.getVn()).append("|")
                        .append(item.isAvailable() ? "Available" : "Borrowed");
                }

                writer.println(line.toString());
            }

            System.out.println("✓ Library catalog saved to: " + filename);
            System.out.println("  Total items saved: " + catalog.size());
            return true;

        } catch (IOException e) {
            System.out.println("✗ Error saving catalog: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean loadFromFile(String filename) {
        int loadedCount = 0, errorCount = 0;

        String fullPath = "data/" + filename;
        File file = new File(fullPath);

        if (!file.exists()) {
            System.out.println("✗ File not found: " + fullPath);
            return false;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(fullPath))) {

            String line;
            catalog.clear();
            borrowCountMap.clear();
            genres.clear();

            while ((line = reader.readLine()) != null) {
                try {
                    String[] parts = line.split("\\|");
                    if (parts.length < 3) { errorCount++; continue; }

                    String type   = parts[0].trim();
                    String itemId = parts[1].trim();
                    String title  = parts[2].trim();
                    LibraryItem item = null;

                    if (type.equals("DigitalBook")) {
                        if (parts.length >= 11) {
                            item = new DigitalBook(itemId, title,
                                    parts[3].trim(), parts[4].trim(),
                                    Integer.parseInt(parts[5].trim()),
                                    parts[6].trim(),
                                    Integer.parseInt(parts[7].trim()),
                                    Double.parseDouble(parts[8].trim()),
                                    parts[9].trim(), parts[10].trim());
                        } else { errorCount++; }

                    } else if (type.equals("Book")) {
                        if (parts.length >= 9) {
                            item = new Book(itemId, title,
                                    parts[3].trim(), parts[4].trim(),
                                    Integer.parseInt(parts[5].trim()),
                                    parts[6].trim(),
                                    Integer.parseInt(parts[7].trim()));
                            if (parts[8].trim().equalsIgnoreCase("Borrowed"))
                                item.markAsBorrowed();
                            // Chapter 1: track author in genres set
                            genres.add(parts[3].trim());
                        } else { errorCount++; }

                    } else if (type.equals("DVD")) {
                        if (parts.length >= 8) {
                            item = new DVD(itemId, title,
                                    Integer.parseInt(parts[4].trim()),
                                    parts[3].trim(),
                                    Integer.parseInt(parts[5].trim()),
                                    parts[6].trim());
                            if (parts[7].trim().equalsIgnoreCase("Borrowed"))
                                item.markAsBorrowed();
                        } else { errorCount++; }

                    } else if (type.equals("Magazine")) {
                        if (parts.length >= 8) {
                            item = new Magazine(itemId, title,
                                    Integer.parseInt(parts[4].trim()),
                                    parts[3].trim(), parts[5].trim(),
                                    Integer.parseInt(parts[6].trim()));
                            if (parts[7].trim().equalsIgnoreCase("Borrowed"))
                                item.markAsBorrowed();
                        } else { errorCount++; }

                    } else { errorCount++; }

                    if (item != null) {
                        catalog.put(item.getItemId(), item);
                        borrowCountMap.put(item.getItemId(), 0);
                        loadedCount++;
                    }

                } catch (Exception e) {
                    errorCount++;
                }
            }

            System.out.println("✓ Loaded " + loadedCount + " items from: " + fullPath);
            if (errorCount > 0)
                System.out.println("⚠️  Skipped " + errorCount + " items due to errors");
            return true;

        } catch (IOException e) {
            System.out.println("✗ Error loading catalog: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void clear() {
        int count = catalog.size();
        catalog.clear();
        borrowCountMap.clear();
        genres.clear();
        System.out.println("✓ Library catalog cleared. Removed " + count + " items.");
    }

    public int getItemCountByType(String type) {
        int count = 0;
        for (LibraryItem item : catalog.values()) {
            if (item.getClass().getSimpleName().equalsIgnoreCase(type)) count++;
        }
        return count;
    }

    public boolean isEmpty()      { return catalog.isEmpty(); }
    public int getCatalogSize()   { return catalog.size(); }
}
