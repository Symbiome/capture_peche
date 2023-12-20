package fr.inrae.fishola.rest.licences;

import fr.inrae.fishola.entities.enums.LicenceType;

import java.time.LocalDate;
import java.util.UUID;

public class LicenceResponseBean {
    public UUID id;
    public String name;
    public UUID userId;
    public LocalDate expirationDate;
    public LicenceType type;
}
