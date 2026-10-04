package com.library.utils;
import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class FileManager {
	   
	    private static final String DATA_DIR = "data/";
	    
	    // Ensure data directory exists
	    static {
	        File dir = new File(DATA_DIR);
	        if (!dir.exists()) {
	            dir.mkdirs();
	        }
	    }
	    
	    // save any list to file
	    public static <T> boolean saveToFile(List<T> items, String filename, DataConverter<T> converter) {
	        try (PrintWriter writer = new PrintWriter(new FileWriter(DATA_DIR + filename))) {
	            for (T item : items) {
	                writer.println(converter.toFileLine(item));
	            }
	            System.out.println("✓ Saved " + items.size() + " records to: " + filename);
	            return true;
	        } catch (IOException e) {
	            System.err.println("✗ Error saving to " + filename + ": " + e.getMessage());
	            return false;
	        }
	    }
	    
	    // load from file
	    public static <T> List<T> loadFromFile(String filename, DataConverter<T> converter) {
	        List<T> items = new ArrayList<>();
	        File file = new File(DATA_DIR + filename);
	        
	        if (!file.exists()) {
	            System.out.println("ℹ️  File not found: " + filename + " (creating new)");
	            return items;
	        }
	        
	        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
	            String line;
	            int lineNum = 0;
	            
	            while ((line = reader.readLine()) != null) {
	                lineNum++;
	                try {
	                    T item = converter.fromFileLine(line);
	                    if (item != null) {
	                        items.add(item);
	                    }
	                } catch (Exception e) {
	                    System.err.println("Warning: Skipping line " + lineNum + " in " + filename + ": " + e.getMessage());
	                }
	            }
	            
	            System.out.println("✓ Loaded " + items.size() + " records from: " + filename);
	            return items;
	            
	        } catch (IOException e) {
	            System.err.println("✗ Error loading from " + filename + ": " + e.getMessage());
	            return items;
	        }
	    }
	    
	    // Export statistics to CSV
	    public static boolean exportToCSV(String filename, String[] headers, List<String[]> data) {
	        try (PrintWriter writer = new PrintWriter(new FileWriter(DATA_DIR + filename))) {
	            
	            // Write headers
	            writer.println(String.join(",", headers));
	            
	            // Write data rows
	            for (String[] row : data) {
	                writer.println(String.join(",", row));
	            }
	            
	            System.out.println("✓ Exported " + data.size() + " rows to CSV: " + filename);
	            return true;
	            
	        } catch (IOException e) {
	            System.err.println("✗ Error exporting CSV " + filename + ": " + e.getMessage());
	            return false;
	        }
	    }
	    
	    // Backup all files
	    public static boolean backupAllFiles() {
	        String backupDir = DATA_DIR + "backup_" + LocalDate.now() + "/";
	        File dir = new File(backupDir);
	        
	        if (!dir.exists()) {
	            dir.mkdirs();
	        }
	        
	        File dataFolder = new File(DATA_DIR);
	        File[] files = dataFolder.listFiles((d, name) -> name.endsWith(".txt"));
	        
	        if (files == null || files.length == 0) {
	            System.out.println("ℹ️  No files to backup.");
	            return false;
	        }
	        
	        int count = 0;
	        for (File file : files) {
	            try {
	                copyFile(file, new File(backupDir + file.getName()));
	                count++;
	            } catch (IOException e) {
	                System.err.println("✗ Error backing up " + file.getName() + ": " + e.getMessage());
	            }
	        }
	        
	        System.out.println("✓ Backed up " + count + " files to: " + backupDir);
	        return true;
	    }
	    
	    // Helper: Copy file
	    private static void copyFile(File source, File dest) throws IOException {
	        try (BufferedReader reader = new BufferedReader(new FileReader(source));
	             PrintWriter writer = new PrintWriter(new FileWriter(dest))) {
	            
	            String line;
	            while ((line = reader.readLine()) != null) {
	                writer.println(line);
	            }
	        }
	    }
	    
	    // Interface for converting objects to/from file lines
	    public interface DataConverter<T> {
	        String toFileLine(T item);
	        T fromFileLine(String line) throws Exception;
	    }
}
