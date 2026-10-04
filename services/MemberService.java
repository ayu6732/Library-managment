package com.library.services;

import com.library.models.persons.*;
import com.library.utils.*;

import java.util.*;
import java.io.*;

public class MemberService extends Service<Person> {

    private Map<String, Person>  members;
    private Map<String, Integer> memberTypeCount;
    private Map<String, String>  customPasswords;

    public MemberService() {
        this.members         = new LinkedHashMap<>();
        this.memberTypeCount = new HashMap<>();
        this.customPasswords = new HashMap<>();
        System.out.println("✓ MemberService initialized.");
    }

    public void setPassword(String memberId, String newPassword) {
        customPasswords.put(memberId.toUpperCase(), newPassword);
    }

    public String getPassword(String memberId) {
        String key = memberId.toUpperCase();
        if (customPasswords.containsKey(key)) return customPasswords.get(key);
        return com.library.gui.LoginPanel.generatePassword(memberId);
    }

    public boolean registerMember(Person person) {
        if (person == null) {
            System.out.println("✗ Cannot register null member.");
            return false;
        }
        if (members.containsKey(person.getId())) {
            System.out.println("✗ Member with ID " + person.getId() + " already exists.");
            return false;
        }
        members.put(person.getId(), person);
        String type = person.getClass().getSimpleName();
        memberTypeCount.put(type, memberTypeCount.getOrDefault(type, 0) + 1);
        System.out.println("✓ Member registered: " + person.getName());
        return true;
    }

    public boolean deleteMember(String memberId) {
        Person member = members.get(memberId);
        if (member == null) {
            System.out.println("✗ Member not found: " + memberId);
            return false;
        }
        String type = member.getClass().getSimpleName();
        memberTypeCount.put(type, Math.max(0, memberTypeCount.getOrDefault(type, 1) - 1));
        members.remove(memberId);
        customPasswords.remove(memberId.toUpperCase());
        System.out.println("✓ Member deleted: " + member.getName());
        return true;
    }

    public boolean updateMember(String memberId, Person updatedMember) {
        if (!members.containsKey(memberId)) {
            System.out.println("✗ Member not found: " + memberId);
            return false;
        }
        members.put(memberId, updatedMember);
        System.out.println("✓ Member updated: " + updatedMember.getName());
        return true;
    }

    @Override
    public Person findById(String memberId) {
        return members.get(memberId);
    }

    public Person findMemberById(String memberId) {
        return members.get(memberId);
    }

    public List<Person> getAllMembers() {
        return new ArrayList<>(members.values());
    }

    public Map<String, Integer> getMemberTypeCount() {
        return Collections.unmodifiableMap(memberTypeCount);
    }

    public void displayMemberLoanHistory(String memberId) {
        Person member = members.get(memberId);
        if (member == null) {
            System.out.println("✗ Member not found: " + memberId);
            return;
        }
        System.out.println("\n=== Loan History for " + member.getName() + " ===");
        member.displayInfo();
    }

    @Override
    public void displayAll() {
        if (members.isEmpty()) {
            System.out.println("\n📋 No members registered.\n");
            return;
        }
        System.out.println("\n=== ALL REGISTERED MEMBERS ===");
        System.out.println("Total Members: " + members.size());
        for (Person person : members.values()) {
            if (person instanceof Professor)    System.out.println("👨‍🏫 Professor:");
            else if (person instanceof Student) System.out.println("👨‍🎓 Student:");
            else if (person instanceof Staff)   System.out.println("👨‍💼 Staff:");
            else if (person instanceof Member)  System.out.println("👤 Member:");
            person.displayInfo();
            System.out.println("----------------------------------------");
        }
        System.out.println("\n=== MEMBER STATISTICS ===");
        for (Map.Entry<String, Integer> entry : memberTypeCount.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }

    @Override
    public void displayStatistics() {
        System.out.println("\n=== MEMBER STATISTICS ===");
        System.out.println("📊 Total Members: " + members.size());
        for (Map.Entry<String, Integer> entry : memberTypeCount.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }
        if (!members.isEmpty()) {
            System.out.println("\n=== LOAN LIMITS ===");
            System.out.println("Professors:    10 items maximum");
            System.out.println("Staff:          7 items maximum");
            System.out.println("Students:       5 items maximum");
            System.out.println("Other Members:  5 items maximum");
        }
    }

    @Override
    public boolean saveToFile(String filename) {
        return FileManager.saveToFile(new ArrayList<>(members.values()), filename,
                new FileManager.DataConverter<Person>() {
                    @Override
                    public String toFileLine(Person person) {
                        StringBuilder line = new StringBuilder();
                        line.append(person.getClass().getSimpleName()).append("|")
                            .append(person.getId()).append("|")
                            .append(person.getName()).append("|")
                            .append(person.getEmail()).append("|")
                            .append(person.getPhoneNumber()).append("|")
                            .append(person.getAddress());
                        if (person instanceof Professor) {
                            Professor prof = (Professor) person;
                            line.append("|").append(prof.getDepartment())
                                .append("|").append(prof.getSpeciality());
                        } else if (person instanceof Student) {
                            Student student = (Student) person;
                            line.append("|").append(student.getDepartment())
                                .append("|").append(student.getMaxLoanLimit());
                        } else if (person instanceof Staff) {
                            Staff staff = (Staff) person;
                            line.append("|").append(staff.getPosition())
                                .append("|").append(staff.getMaxLoanLimit());
                        } else if (person instanceof Member) {
                            Member member = (Member) person;
                            line.append("|").append(member.getPosition())
                                .append("|").append(member.getMaxLoanLimit());
                        }
                        // Always write PWD: field so it round-trips correctly
                        String customPwd = customPasswords.get(person.getId().toUpperCase());
                        line.append("|PWD:").append(customPwd != null ? customPwd : "");
                        return line.toString();
                    }
                    @Override
                    public Person fromFileLine(String line) throws Exception {
                        return parsePerson(line);
                    }
                });
    }

    @Override
    public boolean loadFromFile(String filename) {
        // FIX: clear customPasswords BEFORE loading so stale in-memory passwords
        // never override the passwords that are saved in the file.
        customPasswords.clear();

        List<Person> loadedMembers = FileManager.loadFromFile(filename,
                new FileManager.DataConverter<Person>() {
                    @Override
                    public String toFileLine(Person person) { return ""; }
                    @Override
                    public Person fromFileLine(String line) throws Exception {
                        return parsePerson(line);
                    }
                });
        if (!loadedMembers.isEmpty()) {
            members.clear();
            memberTypeCount.clear();
            for (Person p : loadedMembers) {
                members.put(p.getId(), p);
                String type = p.getClass().getSimpleName();
                memberTypeCount.put(type, memberTypeCount.getOrDefault(type, 0) + 1);
            }
            System.out.println("✓ Loaded " + members.size() + " members from: " + filename);
            return true;
        }
        return false;
    }

    @Override
    public void clear() {
        members.clear();
        memberTypeCount.clear();
        customPasswords.clear();
        System.out.println("✓ All members cleared from the system.");
    }

    private Person parsePerson(String line) throws Exception {
        String[] parts = line.split("\\|");
        if (parts.length < 6) return null;

        // Extract optional custom password — last field starts with "PWD:"
        String customPwd = null;
        String last = parts[parts.length - 1];
        if (last.startsWith("PWD:")) {
            customPwd = last.substring(4);
            String[] trimmed = new String[parts.length - 1];
            System.arraycopy(parts, 0, trimmed, 0, trimmed.length);
            parts = trimmed;
        }

        String type    = parts[0];
        String id      = parts[1];
        String name    = parts[2];
        String email   = parts[3];
        String phone   = parts[4];
        String address = parts[5];

        Person result = null;
        switch (type) {
            case "Professor":
                if (parts.length >= 8)
                    result = new Professor(id, name, email, phone, address, parts[6], parts[7]);
                break;
            case "Student":
                if (parts.length >= 8)
                    result = new Student(id, name, email, phone, address, Integer.parseInt(parts[7]), parts[6]);
                break;
            case "Staff":
                if (parts.length >= 7)
                    result = new Staff(id, name, email, phone, address, parts[6]);
                break;
            case "Member":
                if (parts.length >= 7)
                    result = new Member(id, name, email, phone, address, parts[6]);
                break;
        }

        // Load the saved password back into the map (even if empty string — skip empty)
        if (result != null && customPwd != null && !customPwd.isEmpty()) {
            customPasswords.put(id.toUpperCase(), customPwd);
        }
        return result;
    }
}