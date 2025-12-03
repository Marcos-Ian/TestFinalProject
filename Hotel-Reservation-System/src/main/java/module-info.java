module hotel.reservation.system {
    requires java.logging;
    requires jakarta.persistence;
    // JavaFX dependencies can be added to the module path when the UI is implemented
    // requires javafx.controls;
    // requires javafx.fxml;
    // requires javafx.graphics;
    // requires javafx.base;
    exports app;
    exports service;
    exports service.strategy;
    exports service.decorator;
    exports service.factory;
    exports config;
    exports model;
    exports repository;
    exports security;
    exports util;
    exports events;

    opens model to jakarta.persistence;
}
