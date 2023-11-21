package fr.inrae.fishola.rest.trips;


import java.util.UUID;

public class ExportBean {
    public String nomDeLaPlateforme;
    public String dateDeLaSortie;
    public String typeDePeche;
    public String especeCapturee;
    public Integer longueurTotaleDuPoisson;
    public Integer longueurTotaleDuPoissonCalculee;
    public Integer poidsDuPoisson;
    public String aExclure;
    public UUID catchId;
    /*
     Unused fields in B.O (no need to add them in the the Bean)
    public UUID idLogin;
    public String moisDeLaSortie;
    public String anneeDeLaSortie;
    public UUID idSortie;
    public String especeRecherchee;
    public String debutDePeche;
    public String finDePeche;
    public Integer dureeDeLaSortie;
    public String techniqueDePecheParSortie;
    public UUID idCapture;
    public String techniqueDePecheParCapture;
    public String poissonRelache;
    public UUID idPrelevement;
    public String conditionsMeteo;
    public String modeDePeche;
    public String nomDuProjet;
    public String nomDuSite;
     */
}
