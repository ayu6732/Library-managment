package com.library.services;


public abstract class Service<T> {

    public abstract void displayAll();

    public abstract boolean saveToFile(String filename);

    public abstract boolean loadFromFile(String filename);

    public abstract void displayStatistics();

    public abstract void clear();

   
    public abstract T findById(String id);
}
