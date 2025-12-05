package model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Guest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private String loyaltyNumber;
    private String street;
    private String city;
    private String province;
    private String postalCode;

    @OneToMany(mappedBy = "guest")
    private List<Reservation> reservations = new ArrayList<>();

    public Long getId() { return id; }

    public String getFirstName() { return firstName; }

    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }

    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPhone() { return phone; }

    public String getPhoneNumber() { return phone; }

    public void setPhone(String phone) { this.phone = phone; }

    public void setPhoneNumber(String phone) { this.phone = phone; }

    public String getEmail() { return email; }

    public void setEmail(String email) { this.email = email; }

    public String getLoyaltyNumber() { return loyaltyNumber; }

    public void setLoyaltyNumber(String loyaltyNumber) { this.loyaltyNumber = loyaltyNumber; }

    public List<Reservation> getReservations() { return reservations; }

    public void setReservations(List<Reservation> reservations) { this.reservations = reservations; }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }
}
