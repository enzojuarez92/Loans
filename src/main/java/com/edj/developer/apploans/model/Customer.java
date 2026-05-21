package com.edj.developer.apploans.model;

import javafx.beans.property.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Customer — Modelo de dominio con JavaFX Properties.
 *
 * El uso de Properties (en lugar de campos simples) permite:
 *  - Binding directo con TableView sin CellValueFactory manual
 *  - FilteredList reactiva sin conversiones
 *  - Actualizaciones de UI automáticas al modificar datos
 */
public class Customer {

    private final IntegerProperty id         = new SimpleIntegerProperty();
    private final StringProperty  docNumber  = new SimpleStringProperty();
    private final StringProperty  firstName  = new SimpleStringProperty();
    private final StringProperty  lastName   = new SimpleStringProperty();
    private final StringProperty  phone      = new SimpleStringProperty();
    private final StringProperty  email      = new SimpleStringProperty();
    private final StringProperty  address    = new SimpleStringProperty();
    private final StringProperty  city       = new SimpleStringProperty();
    private final StringProperty  status     = new SimpleStringProperty("ACTIVE");
    private final StringProperty  notes      = new SimpleStringProperty();

    private final ObjectProperty<LocalDate>     birthDate  = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> createdAt  = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> updatedAt  = new SimpleObjectProperty<>();

    // ─── Constructores ────────────────────────────────────────────────────

    public Customer() {}

    public Customer(int id, String docNumber, String firstName, String lastName,
                    String phone, String email, String address, String city,
                    String status, LocalDate birthDate, LocalDateTime createdAt) {
        this.id.set(id);
        this.docNumber.set(docNumber);
        this.firstName.set(firstName);
        this.lastName.set(lastName);
        this.phone.set(phone);
        this.email.set(email);
        this.address.set(address);
        this.city.set(city);
        this.status.set(status);
        this.birthDate.set(birthDate);
        this.createdAt.set(createdAt);
    }

    // ─── Métodos utilitarios ──────────────────────────────────────────────

    /** Nombre completo para mostrar en la tabla */
    public String getFullName() {
        return (nullSafe(firstName.get()) + " " + nullSafe(lastName.get())).trim();
    }

    /**
     * Texto unificado para FilteredList.
     * Busca en: docNumber, nombres, teléfono, email, ciudad.
     */
    public String getSearchableText() {
        return String.join(" ",
                nullSafe(docNumber.get()),
                nullSafe(firstName.get()),
                nullSafe(lastName.get()),
                nullSafe(phone.get()),
                nullSafe(email.get()),
                nullSafe(city.get())
        ).toLowerCase();
    }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(getStatus());
    }

    private String nullSafe(String s) { return s == null ? "" : s; }

    @Override
    public String toString() {
        return "Customer{id=%d, doc='%s', name='%s'}".formatted(
                getId(), getDocNumber(), getFullName());
    }

    // ─── Getters / Setters / Property accessors ───────────────────────────

    public int getId()                              { return id.get(); }
    public void setId(int v)                        { id.set(v); }
    public IntegerProperty idProperty()             { return id; }

    public String getDocNumber()                    { return docNumber.get(); }
    public void setDocNumber(String v)              { docNumber.set(v); }
    public StringProperty docNumberProperty()       { return docNumber; }

    public String getFirstName()                    { return firstName.get(); }
    public void setFirstName(String v)              { firstName.set(v); }
    public StringProperty firstNameProperty()       { return firstName; }

    public String getLastName()                     { return lastName.get(); }
    public void setLastName(String v)               { lastName.set(v); }
    public StringProperty lastNameProperty()        { return lastName; }

    public String getPhone()                        { return phone.get(); }
    public void setPhone(String v)                  { phone.set(v); }
    public StringProperty phoneProperty()           { return phone; }

    public String getEmail()                        { return email.get(); }
    public void setEmail(String v)                  { email.set(v); }
    public StringProperty emailProperty()           { return email; }

    public String getAddress()                      { return address.get(); }
    public void setAddress(String v)                { address.set(v); }
    public StringProperty addressProperty()         { return address; }

    public String getCity()                         { return city.get(); }
    public void setCity(String v)                   { city.set(v); }
    public StringProperty cityProperty()            { return city; }

    public String getStatus()                       { return status.get(); }
    public void setStatus(String v)                 { status.set(v); }
    public StringProperty statusProperty()          { return status; }

    public String getNotes()                        { return notes.get(); }
    public void setNotes(String v)                  { notes.set(v); }
    public StringProperty notesProperty()           { return notes; }

    public LocalDate getBirthDate()                 { return birthDate.get(); }
    public void setBirthDate(LocalDate v)           { birthDate.set(v); }
    public ObjectProperty<LocalDate> birthDateProperty() { return birthDate; }

    public LocalDateTime getCreatedAt()             { return createdAt.get(); }
    public void setCreatedAt(LocalDateTime v)       { createdAt.set(v); }
    public ObjectProperty<LocalDateTime> createdAtProperty() { return createdAt; }

    public LocalDateTime getUpdatedAt()             { return updatedAt.get(); }
    public void setUpdatedAt(LocalDateTime v)       { updatedAt.set(v); }
    public ObjectProperty<LocalDateTime> updatedAtProperty() { return updatedAt; }
}