
// ═════════════════════════════════════════════════════════════════════════════
// ARCHIVO 2: src/main/java/com/edj/developer/apploans/util/SessionManager.java
// ═════════════════════════════════════════════════════════════════════════════
package com.edj.developer.apploans.util;

import com.edj.developer.apploans.model.User;

/**
 * SessionManager — Singleton que mantiene la sesión del usuario activo.
 *
 * Patrón: Bill Pugh Singleton (thread-safe sin synchronized).
 *
 * Uso:
 *   // Al hacer login:
 *   SessionManager.getInstance().setCurrentUser(user);
 *
 *   // Desde cualquier controlador:
 *   User u = SessionManager.getInstance().getCurrentUser();
 *
 *   // Al hacer logout:
 *   SessionManager.getInstance().clearSession();
 */
public final class SessionManager {

    private User currentUser;

    private SessionManager() {}

    /** Bill Pugh holder — inicialización lazy y thread-safe */
    private static final class Holder {
        private static final SessionManager INSTANCE = new SessionManager();
    }

    public static SessionManager getInstance() {
        return Holder.INSTANCE;
    }

    /** Retorna el usuario actualmente autenticado, o null si no hay sesión */
    public User getCurrentUser() {
        return currentUser;
    }

    /** Establece el usuario de la sesión activa (llamar después de login exitoso) */
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    /** Elimina la sesión activa (llamar en logout) */
    public void clearSession() {
        this.currentUser = null;
    }

    /** Retorna true si hay una sesión activa */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /** Retorna true si el usuario actual tiene rol ADMIN */
    public boolean isAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }
}
