// ═════════════════════════════════════════════════════════════════════════════
// ARCHIVO 1: src/main/java/com/edj/developer/apploans/model/User.java
// ═════════════════════════════════════════════════════════════════════════════
package com.edj.developer.apploans.model;

/**
 * User — Modelo del usuario autenticado.
 *
 * Usado exclusivamente para la sesión activa (SessionManager).
 * NO usa JavaFX Properties porque no se muestra en TableView.
 */
public class User {

    private int     id;
    private String  username;
    private String  fullName;
    private String  role;
    private boolean active;

    public User() {}

    public User(int id, String username, String fullName, String role, boolean active) {
        this.id       = id;
        this.username = username;
        this.fullName = fullName;
        this.role     = role;
        this.active   = active;
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    @Override
    public String toString() {
        return "User{id=%d, username='%s', role='%s'}".formatted(id, username, role);
    }

    // ─── Getters / Setters ────────────────────────────────────────────────
    public int     getId()               { return id; }
    public void    setId(int id)         { this.id = id; }

    public String  getUsername()         { return username; }
    public void    setUsername(String v) { this.username = v; }

    public String  getFullName()         { return fullName; }
    public void    setFullName(String v) { this.fullName = v; }

    public String  getRole()             { return role; }
    public void    setRole(String v)     { this.role = v; }

    public boolean isActive()            { return active; }
    public void    setActive(boolean v)  { this.active = v; }
}


