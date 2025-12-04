package service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lightweight waitlist manager to demonstrate admin workflows without
 * modifying the domain logic.
 */
public class WaitlistService {
    private final List<WaitlistEntry> entries = new ArrayList<>();

    public void addToWaitlist(String guestName, String roomType, LocalDate desiredDate) {
        entries.add(new WaitlistEntry(guestName, roomType, desiredDate));
    }

    public void removeFromWaitlist(WaitlistEntry entry) {
        entries.remove(entry);
    }

    public List<WaitlistEntry> listEntries() {
        return Collections.unmodifiableList(entries);
    }

    public static class WaitlistEntry {
        private final String guestName;
        private final String roomType;
        private final LocalDate desiredDate;

        public WaitlistEntry(String guestName, String roomType, LocalDate desiredDate) {
            this.guestName = guestName;
            this.roomType = roomType;
            this.desiredDate = desiredDate;
        }

        public String getGuestName() {
            return guestName;
        }

        public String getRoomType() {
            return roomType;
        }

        public LocalDate getDesiredDate() {
            return desiredDate;
        }
    }
}
