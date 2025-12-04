package service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lightweight in-memory feedback store to support the presentation layer.
 * This keeps feedback handling out of the core business logic while giving
 * controllers a consistent API.
 */
public class FeedbackService {
    private final List<FeedbackEntry> entries = new ArrayList<>();

    public FeedbackEntry submitFeedback(Long reservationId, String guestName, int rating, String comments) {
        FeedbackEntry entry = new FeedbackEntry(reservationId, guestName, rating, comments, LocalDate.now());
        entries.add(entry);
        return entry;
    }

    public List<FeedbackEntry> listFeedback() {
        return Collections.unmodifiableList(entries);
    }

    public static class FeedbackEntry {
        private final Long reservationId;
        private final String guestName;
        private final int rating;
        private final String comments;
        private final LocalDate submittedOn;

        public FeedbackEntry(Long reservationId, String guestName, int rating, String comments, LocalDate submittedOn) {
            this.reservationId = reservationId;
            this.guestName = guestName;
            this.rating = rating;
            this.comments = comments;
            this.submittedOn = submittedOn;
        }

        public Long getReservationId() {
            return reservationId;
        }

        public String getGuestName() {
            return guestName;
        }

        public int getRating() {
            return rating;
        }

        public String getComments() {
            return comments;
        }

        public LocalDate getSubmittedOn() {
            return submittedOn;
        }
    }
}
