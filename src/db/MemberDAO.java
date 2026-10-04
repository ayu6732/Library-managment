package com.library.db;

import com.library.models.persons.*;
import java.sql.*;
import java.util.*;

public class MemberDAO {

    public static boolean insertMember(Person person) {
        String sql = "INSERT OR IGNORE INTO members (id, type, name, email, phone, address, extra_field1, extra_field2) VALUES (?,?,?,?,?,?,?,?)";
        try {
            String type=person.getClass().getSimpleName(), ef1="", ef2="";
            if (person instanceof Professor) { Professor p=(Professor)person; ef1=p.getDepartment(); ef2=p.getSpeciality(); }
            else if (person instanceof Student) { Student s=(Student)person; ef1=s.getDepartment(); ef2=String.valueOf(s.getMaxLoanLimit()); }
            else if (person instanceof Staff) { Staff s=(Staff)person; ef1=s.getPosition(); ef2=String.valueOf(s.getMaxLoanLimit()); }
            else if (person instanceof Member) { Member m=(Member)person; ef1=m.getPosition(); ef2=String.valueOf(m.getMaxLoanLimit()); }
            return DatabaseManager.executeUpdate(sql, person.getId(), type, person.getName(), person.getEmail(), person.getPhoneNumber(), person.getAddress(), ef1, ef2) > 0;
        } catch (SQLException e) { System.err.println("✗ MemberDAO.insertMember: " + e.getMessage()); return false; }
    }

    public static boolean deleteMember(String memberId) {
        try { return DatabaseManager.executeUpdate("DELETE FROM members WHERE id = ?", memberId) > 0; }
        catch (SQLException e) { System.err.println("✗ MemberDAO.deleteMember: " + e.getMessage()); return false; }
    }

    public static List<Person> getAllMembers() {
        List<Person> list = new ArrayList<>();
        try {
            ResultSet rs = DatabaseManager.executeQuery("SELECT * FROM members");
            while (rs.next()) { Person p = mapRow(rs); if (p != null) list.add(p); }
            rs.close();
        } catch (SQLException e) { System.err.println("✗ MemberDAO.getAllMembers: " + e.getMessage()); }
        return list;
    }

    public static Person getMemberById(String memberId) {
        try {
            ResultSet rs = DatabaseManager.executeQuery("SELECT * FROM members WHERE id = ?", memberId);
            if (rs.next()) { Person p = mapRow(rs); rs.close(); return p; }
            rs.close();
        } catch (SQLException e) { System.err.println("✗ MemberDAO.getMemberById: " + e.getMessage()); }
        return null;
    }

    public static void syncAll(List<Person> members) {
        for (Person p : members) insertMember(p);
        System.out.println("✓ MemberDAO: synced " + members.size() + " members to database.");
    }

    private static Person mapRow(ResultSet rs) throws SQLException {
        String type=rs.getString("type"), id=rs.getString("id"), name=rs.getString("name"),
               email=rs.getString("email"), phone=rs.getString("phone"), address=rs.getString("address"),
               ef1=rs.getString("extra_field1"), ef2=rs.getString("extra_field2");
        try {
            switch (type) {
                case "Professor": return new Professor(id, name, email, phone, address, ef1, ef2);
                case "Student":   return new Student(id, name, email, phone, address, ef2!=null?Integer.parseInt(ef2):5, ef1);
                case "Staff":     return new Staff(id, name, email, phone, address, ef1);
                case "Member":    return new Member(id, name, email, phone, address, ef1);
            }
        } catch (Exception e) { System.err.println("✗ MemberDAO.mapRow error for " + id + ": " + e.getMessage()); }
        return null;
    }
}