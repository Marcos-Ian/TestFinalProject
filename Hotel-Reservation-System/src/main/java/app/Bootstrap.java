package app;

import security.AuthenticationService;
import app.config.EntityManagerProvider;
import config.LoyaltyConfig;
import config.PricingConfig;
import events.RoomAvailabilitySubject;
import repository.GuestRepository;
import repository.RoomRepository;
import repository.ReservationRepository;
import repository.FeedbackRepository;
import repository.impl.GuestRepositoryImpl;
import repository.impl.RoomRepositoryImpl;
import repository.impl.ReservationRepositoryImpl;
import repository.impl.FeedbackRepositoryImpl;
import service.BillingContext;
import service.FeedbackService;
import service.GuestService;
import service.LoyaltyService;
import service.ReservationService;
import service.RoomService;
import service.strategy.StandardBillingStrategy;
import util.LoggingProvider;

import jakarta.persistence.EntityManager;
import java.util.logging.Logger;

/**
 * Application bootstrap responsible for wiring dependencies and starting the JavaFX UI.
 * This class initializes all core services, repositories, and configuration objects
 * following dependency injection principles.
 */
public class Bootstrap {
    private static final Logger LOGGER = Logger.getLogger(Bootstrap.class.getName());

    // Application-wide service instances
    private static ReservationService reservationService;
    private static RoomService roomService;
    private static LoyaltyService loyaltyService;
    private static BillingContext billingContext;
    private static GuestService guestService;
    private static AuthenticationService authenticationService;
    private static FeedbackService feedbackService;

    // Configuration instances
    private static PricingConfig pricingConfig;
    private static LoyaltyConfig loyaltyConfig;

    // Event subjects
    private static RoomAvailabilitySubject roomAvailabilitySubject;

    public static void main(String[] args) {
        try {
            LOGGER.info("Starting Hotel Reservation System...");

            // Initialize logging
            LoggingProvider.configure();

            // Initialize persistence
            EntityManager entityManager = EntityManagerProvider.getEntityManager();
            LOGGER.info("EntityManager initialized successfully");

            // Initialize configuration objects
            initializeConfigurations();

            // Initialize repositories
            GuestRepository guestRepository = new GuestRepositoryImpl(entityManager);
            RoomRepository roomRepository = new RoomRepositoryImpl(entityManager);
            ReservationRepository reservationRepository = new ReservationRepositoryImpl(entityManager);
            FeedbackRepository feedbackRepository = new FeedbackRepositoryImpl(entityManager);
            LOGGER.info("Repositories initialized");

            initializeRoomTypes(roomRepository);

            // Initialize event system
            roomAvailabilitySubject = new RoomAvailabilitySubject();

            authenticationService = new AuthenticationService();
            LOGGER.info("Authentication service initialized");

            // Initialize services
            reservationService = new ReservationService(guestRepository, reservationRepository, roomRepository, authenticationService);
            guestService = new GuestService(guestRepository, reservationRepository);
            roomService = new RoomService(roomAvailabilitySubject, roomRepository, reservationRepository);
            loyaltyService = new LoyaltyService(loyaltyConfig);
            feedbackService = new FeedbackService(reservationRepository, feedbackRepository);
            LOGGER.info("Services initialized");

            // Initialize billing context with default strategy
            billingContext = new BillingContext(pricingConfig);
            billingContext.setStrategy(new StandardBillingStrategy(pricingConfig));
            LOGGER.info("Billing context initialized with standard strategy");

            // TODO: Launch JavaFX Application once UI controllers are implemented
            // Application.launch(HotelReservationApp.class, args);

            LOGGER.info("Hotel Reservation System bootstrap initialized successfully.");
            System.out.println("Hotel Reservation System is ready.");
            System.out.println("Pricing - Weekend: " + pricingConfig.getWeekendMultiplier() +
                    "x, Peak Season: " + pricingConfig.getPeakSeasonMultiplier() + "x");
            System.out.println("Loyalty - Earn rate: " + loyaltyConfig.getEarnRate() +
                    " points/$, Redeem cap: " + loyaltyConfig.getRedeemCap() + " points");

        } catch (Exception e) {
            LOGGER.severe("Failed to initialize application: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Initialize configuration objects with default or externalized values.
     */
    private static void initializeConfigurations() {
        pricingConfig = new PricingConfig();
        loyaltyConfig = new LoyaltyConfig();

        // TODO: Load configurations from external properties file or environment variables
        // Properties props = loadProperties("application.properties");
        // pricingConfig.setWeekendMultiplier(Double.parseDouble(props.getProperty("pricing.weekend", "1.2")));

        LOGGER.info("Configurations initialized");
    }

    // Getters for dependency injection into controllers

    public static ReservationService getReservationService() {
        if (reservationService == null) {
            throw new IllegalStateException("Application not initialized. Call main() first.");
        }
        return reservationService;
    }

    public static RoomService getRoomService() {
        if (roomService == null) {
            throw new IllegalStateException("Application not initialized. Call main() first.");
        }
        return roomService;
    }

    public static LoyaltyService getLoyaltyService() {
        if (loyaltyService == null) {
            throw new IllegalStateException("Application not initialized. Call main() first.");
        }
        return loyaltyService;
    }

    public static FeedbackService getFeedbackService() {
        if (feedbackService == null) {
            throw new IllegalStateException("Application not initialized. Call main() first.");
        }
        return feedbackService;
    }

    public static BillingContext getBillingContext() {
        if (billingContext == null) {
            throw new IllegalStateException("Application not initialized. Call main() first.");
        }
        return billingContext;
    }

    public static GuestService getGuestService() {
        if (guestService == null) {
            throw new IllegalStateException("Application not initialized. Call main() first.");
        }
        return guestService;
    }
    public static AuthenticationService getAuthenticationService() {
        if (authenticationService == null) {
            authenticationService = new AuthenticationService();
        }
        return authenticationService;
    }

    public static PricingConfig getPricingConfig() {
        return pricingConfig;
    }

    public static LoyaltyConfig getLoyaltyConfig() {
        return loyaltyConfig;
    }

    public static RoomAvailabilitySubject getRoomAvailabilitySubject() {
        return roomAvailabilitySubject;
    }

    private static void initializeRoomTypes(RoomRepository roomRepository) {
        if (roomRepository == null) {
            return;
        }

        seedRoomType(roomRepository, model.RoomType.Type.SINGLE, 100.0, 2);
        seedRoomType(roomRepository, model.RoomType.Type.DOUBLE, 150.0, 4);
        seedRoomType(roomRepository, model.RoomType.Type.DELUXE, 250.0, 2);
        seedRoomType(roomRepository, model.RoomType.Type.PENTHOUSE, 500.0, 2);
    }

    private static void seedRoomType(RoomRepository roomRepository, model.RoomType.Type type,
                                     double basePrice, int capacity) {
        roomRepository.findByType(type).ifPresentOrElse(
                existing -> LOGGER.fine("Room type already exists: " + type),
                () -> {
                    model.RoomType roomType = service.factory.RoomFactory.create(type, basePrice, capacity);
                    roomRepository.save(roomType);
                    LOGGER.info("Seeded room type: " + type);
                }
        );
    }
}
