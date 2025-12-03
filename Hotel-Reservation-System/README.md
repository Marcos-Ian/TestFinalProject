# Hotel Reservation System Refactor Plan

This repository now contains a structured skeleton for the three-tier JavaFX Hotel Reservation System described in the assignment brief. The previous binary-only output has been replaced with source folders that demonstrate how the application will be organized and wired together.

## Architecture
- **Presentation layer** (`controller`, `view`) keeps JavaFX controllers and FXML assets separated from services.
- **Business layer** (`service`, `config`, `events`, `util`) centralizes pricing, loyalty, validation, notifications, and billing strategies.
- **Data layer** (`model`, `repository`, `security`) hosts JPA entities, repository interfaces, and admin authentication utilities.
- **App bootstrap** (`app`) provides an entry point, dependency wiring, and the shared `EntityManagerFactory` singleton.

## Status
This refactor introduces the packages, configuration placeholders, and documentation for required design patterns (strategy, observer, factory, decorator, singleton). The code is intentionally light-weight to make it easy to fill in domain logic, JPA mappings, and JavaFX scenes without breaking the new package layout.

## How to proceed
1. Add JavaFX FXML and controller implementations into `controller` and `src/main/resources/view` using the documented structure.
2. Implement JPA entities within `model` and connect repositories through `app.Bootstrap`.
3. Flesh out pricing, loyalty, billing strategies, and observer-driven waitlist notifications in the provided service skeletons.
4. Wire authentication using BCrypt in `security` and configure logging via `util.LoggingProvider`.
5. Use the provided exporter and validation utilities as the basis for reporting and UI validation.

The intent is to make the remaining work clearer while preserving compatibility with the assignment specification.
