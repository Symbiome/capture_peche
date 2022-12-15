package fr.inrae.fishola.rest.trips;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class PicturePerTripBean {
    public LocalDate tripDate;
    public UUID tripId;
    public String tripName;
    public String tripLakeName;
    public List<String> pictureURLs;
}
