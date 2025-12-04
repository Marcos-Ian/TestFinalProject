module hotel.reservation.system {
    // JavaFX
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.media;

    // JPA / Hibernate / JDBC
    requires java.sql;
    requires jakarta.persistence;
    requires org.hibernate.orm.core;
    requires org.xerial.sqlitejdbc;

    // 🔑 Open packages for reflection
    // Hibernate needs these to access private fields on your entities
    opens model;       // Guest, Reservation, RoomType, etc.
    opens security;    // AdminUser

    // JavaFX FXML controllers (if any)
    opens controller to javafx.fxml;
    opens view to javafx.fxml;

    // Public API exports
    exports app;
    exports controller;
    exports model;
    exports security;
}
