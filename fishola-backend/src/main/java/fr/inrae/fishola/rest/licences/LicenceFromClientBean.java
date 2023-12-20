package fr.inrae.fishola.rest.licences;

import fr.inrae.fishola.entities.enums.LicenceType;

import java.time.LocalDate;

public class LicenceFromClientBean {
    public String name;
    public LocalDate expirationDate;
    public LicenceType type;
    public String content;
}
