package com.library.services;

import com.library.models.transactions.Reservation;
import com.library.models.transactions.*;
import com.library.models.items.LibraryItem;
import com.library.models.persons.Person;

import java.util.*;
import java.io.*;

public class ReservationService extends Service<Reservation> {

    private LinkedList<Reservation> reservations;
    private Map<String, LinkedList<Reservation>> reservationQueueMap;
    private int nextReservationId;
    private MemberService memberService;
    private LibraryService libraryService;

    public ReservationService(MemberService memberService, LibraryService libraryService) {
        this.reservations         = new LinkedList<>();
        this.reservationQueueMap  = new HashMap<>();
        this.nextReservationId    = 1;
        this.memberService        = memberService;
        this.libraryService       = libraryService;
        System.out.println("✓ ReservationService initialized.");
    }

    public Reservation reserveItem(Person person, LibraryItem item) {
        if (hasActiveReservation(person.getId(), item.getItemId())) {
            System.out.println("✗ " + person.getName() + " already has an active reservation for: " + item.getTitle());
            return null;
        }
        String reservationId = "R" + String.format("%04d", nextReservationId++);
        Reservation reservation = new Reservation(reservationId, person, item);
        reservations.add(reservation);
        reservationQueueMap.computeIfAbsent(item.getItemId(), k -> new LinkedList<>()).add(reservation);
        System.out.println("\n ✓ Reservation created successfully!");
        System.out.println("\n Reservation ID: " + reservationId);
        System.out.println("\n Member: " + person.getName());
        System.out.println("\n Item: " + item.getTitle());
        System.out.println("\n Must pick up by: " + reservation.getExpirationDate());
        int queuePosition = getReservationQueue(item.getItemId()).size();
        System.out.println("\n Queue position: " + queuePosition);
        return reservation;
    }

    public boolean completeReservation(String reservationId) {
        Reservation reservation = findReservationById(reservationId);
        if (reservation == null) { System.out.println("\n ✗ Reservation not found: " + reservationId); return false; }
        reservation.complete();
        removeFromQueueMap(reservation);
        return true;
    }

    public boolean cancelReservation(String reservationId) {
        Reservation reservation = findReservationById(reservationId);
        if (reservation == null) { System.out.println("\n ✗ Reservation not found: " + reservationId); return false; }
        reservation.cancel();
        removeFromQueueMap(reservation);
        return true;
    }

    public boolean notifyMember(String reservationId) {
        Reservation reservation = findReservationById(reservationId);
        if (reservation == null) { System.out.println("\n ✗ Reservation not found: " + reservationId); return false; }
        reservation.notifyPerson();
        return true;
    }

    public boolean processNextReservation(String itemId) {
        LinkedList<Reservation> queue = reservationQueueMap.get(itemId);
        if (queue == null || queue.isEmpty()) { System.out.println("\n ℹ️  No reservations in queue for this item."); return false; }
        Reservation nextReservation = queue.peek();
        nextReservation.notifyPerson();
        System.out.println("\n ✓ Next person in queue has been notified:");
        System.out.println("  " + nextReservation.getPerson().getName());
        return true;
    }

    public List<Reservation> getActiveReservations() {
        List<Reservation> active = new ArrayList<>();
        for (Reservation res : reservations) {
            if (res.getStatus().equals("ACTIVE")) active.add(res);
        }
        return active;
    }

    public List<Reservation> getReservationsForPerson(String personId) {
        List<Reservation> personReservations = new ArrayList<>();
        for (Reservation res : reservations) {
            if (res.getPerson().getId().equals(personId)) personReservations.add(res);
        }
        return personReservations;
    }

    public List<Reservation> getReservationQueue(String itemId) {
        LinkedList<Reservation> queue = reservationQueueMap.get(itemId);
        if (queue == null) return new ArrayList<>();
        List<Reservation> activeQueue = new ArrayList<>();
        for (Reservation res : queue) {
            if (res.getStatus().equals("ACTIVE")) activeQueue.add(res);
        }
        return activeQueue;
    }

    public List<Reservation> getExpiredReservations() {
        List<Reservation> expired = new ArrayList<>();
        for (Reservation res : reservations) {
            if (res.isOverdue() && res.getStatus().equals("ACTIVE")) expired.add(res);
        }
        return expired;
    }

    private boolean hasActiveReservation(String personId, String itemId) {
        LinkedList<Reservation> queue = reservationQueueMap.get(itemId);
        if (queue == null) return false;
        for (Reservation res : queue) {
            if (res.getPerson().getId().equals(personId) && res.getStatus().equals("ACTIVE")) return true;
        }
        return false;
    }

    @Override
    public Reservation findById(String reservationId) { return findReservationById(reservationId); }

    private Reservation findReservationById(String reservationId) {
        for (Reservation res : reservations) {
            if (res.getTransactionId().equals(reservationId)) return res;
        }
        return null;
    }

    private void removeFromQueueMap(Reservation reservation) {
        String itemId = reservation.getItem().getItemId();
        LinkedList<Reservation> queue = reservationQueueMap.get(itemId);
        if (queue != null) {
            queue.remove(reservation);
            if (queue.isEmpty()) reservationQueueMap.remove(itemId);
        }
    }

    @Override
    public void displayAll() {
        if (reservations.isEmpty()) { System.out.println("\n 📋 No reservations recorded.\n"); return; }
        System.out.println("\n ALL RESERVATIONS");
        System.out.println("\n Total: " + reservations.size());
        for (Reservation res : reservations) res.displayInfo();
    }

    public void displayActiveReservations() {
        List<Reservation> active = getActiveReservations();
        if (active.isEmpty()) { System.out.println("\n 📋 No active reservations.\n"); return; }
        System.out.println("\n ACTIVE RESERVATIONS");
        System.out.println("Count: " + active.size());
        for (Reservation res : active) res.displayInfo();
    }

    public void displayExpiredReservations() {
        List<Reservation> expired = getExpiredReservations();
        if (expired.isEmpty()) { System.out.println("\n ✅ No expired reservations!\n"); return; }
        System.out.println("\n ⚠️ EXPIRED RESERVATIONS ⚠️");
        System.out.println("Count: " + expired.size());
        for (Reservation res : expired) res.displayInfo();
    }

    public void displayQueue(String itemId, String itemTitle) {
        List<Reservation> queue = getReservationQueue(itemId);
        if (queue.isEmpty()) { System.out.println("\n 📋 No reservations for: " + itemTitle + "\n"); return; }
        System.out.println("\n RESERVATION QUEUE: " + itemTitle);
        System.out.println("People waiting: " + queue.size());
        int position = 1;
        for (Reservation res : queue) {
            System.out.println("Position " + position + ":");
            System.out.println(" Name: " + res.getPerson().getName());
            System.out.println(" ID: " + res.getTransactionId());
            System.out.println(" Reserved on: " + res.getTransactionDate());
            position++;
        }
    }

    @Override
    public void displayStatistics() {
        int active = 0, completed = 0, cancelled = 0, expired = 0;
        double totalFees = 0.0;
        for (Reservation res : reservations) {
            if (res.getStatus().equals("ACTIVE")) { active++; if (res.isOverdue()) expired++; }
            if (res.getStatus().equals("COMPLETED")) completed++;
            if (res.getStatus().equals("CANCELLED"))  cancelled++;
            totalFees += res.calculateFees();
        }
        System.out.println(" RESERVATION STATISTICS");
        System.out.println(" 📊 Total:           " + reservations.size());
        System.out.println(" ✅ Active:          " + active);
        System.out.println(" ⚠️  Expired:        " + expired);
        System.out.println(" ✔️  Completed:      " + completed);
        System.out.println(" ❌ Cancelled:       " + cancelled);
        System.out.println(" 💰 Fees Collected:  " + String.format("%.2f", totalFees) + " DA");
        System.out.println("\n 📋 Items with most reservations:");
        reservationQueueMap.entrySet().stream()
                .sorted((a, b) -> b.getValue().size() - a.getValue().size())
                .limit(3)
                .forEach(e -> System.out.println("   Item " + e.getKey() + " : " + e.getValue().size() + " in queue"));
    }

    @Override
    public boolean saveToFile(String filename) {
        File dataDir = new File("data");
        if (!dataDir.exists()) dataDir.mkdirs();
        String fullPath = "data/" + filename;
        try (PrintWriter writer = new PrintWriter(new FileWriter(fullPath))) {
            for (Reservation res : reservations) {
                String line = res.getTransactionId() + "|" + res.getPerson().getId() + "|"
                        + res.getItem().getItemId() + "|" + res.getExpirationDate() + "|"
                        + res.isNotified() + "|" + res.getStatus();
                writer.println(line);
            }
            System.out.println("✓ Reservations saved to: " + filename);
            return true;
        } catch (IOException e) {
            System.out.println("✗ Error saving reservations: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean loadFromFile(String filename) {
        String fullPath = "data/" + filename;
        File file = new File(fullPath);
        if (!file.exists()) { System.out.println("ℹ️  Reservations file not found: " + fullPath + " (starting fresh)"); return false; }
        try (BufferedReader reader = new BufferedReader(new FileReader(fullPath))) {
            String line; int loadedCount = 0, errorCount = 0;
            while ((line = reader.readLine()) != null) {
                try {
                    String[] parts = line.split("\\|");
                    if (parts.length < 6) { errorCount++; continue; }
                    String reservationId = parts[0].trim(); String personId = parts[1].trim();
                    String itemId = parts[2].trim(); String expirationDateStr = parts[3].trim();
                    String notifiedStr = parts[4].trim(); String status = parts[5].trim();
                    boolean alreadyExists = false;
                    for (Reservation existing : reservations) {
                        if (existing.getTransactionId().equals(reservationId)) { alreadyExists = true; break; }
                    }
                    if (alreadyExists) continue;
                    Person person = memberService.findMemberById(personId);
                    if (person == null) { errorCount++; continue; }
                    LibraryItem item = libraryService.findItemById(itemId);
                    if (item == null) { errorCount++; continue; }
                    Reservation reservation = new Reservation(reservationId, person, item);
                    java.lang.reflect.Field statusField = Transaction.class.getDeclaredField("status");
                    statusField.setAccessible(true);
                    statusField.set(reservation, status);
                    reservation.setNotified(Boolean.parseBoolean(notifiedStr));
                    try {
                        java.lang.reflect.Field expDateField = Reservation.class.getDeclaredField("expirationDate");
                        expDateField.setAccessible(true);
                        expDateField.set(reservation, java.time.LocalDate.parse(expirationDateStr));
                    } catch (Exception ignored) {}
                    if (reservationId.startsWith("R")) {
                        try {
                            int num = Integer.parseInt(reservationId.substring(1));
                            if (num >= nextReservationId) nextReservationId = num + 1;
                        } catch (NumberFormatException ignored) {}
                    }
                    reservations.add(reservation);
                    if (status.equals("ACTIVE")) {
                        reservationQueueMap.computeIfAbsent(itemId, k -> new LinkedList<>()).add(reservation);
                    }
                    loadedCount++;
                } catch (Exception e) { errorCount++; }
            }
            System.out.println("✓ Successfully loaded " + loadedCount + " reservations from: " + filename);
            if (errorCount > 0) System.out.println("⚠️  Skipped " + errorCount + " reservations due to errors");
            return true;
        } catch (IOException e) {
            System.out.println("✗ Error loading reservations: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void clear() {
        reservations.clear();
        reservationQueueMap.clear();
        nextReservationId = 1;
        System.out.println("✓ All reservations cleared.");
    }
}