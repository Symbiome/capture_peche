package fr.inrae.fishola;

/*
 * #%L
 * SISPEA Utils
 * %%
 * Copyright (C) 2014 - 2023 OFB
 * %%
 * OFB - Tous droits réservés
 * #L%
 */

import org.immutables.value.Value;

/**
 * Cette annotation permet de définir le "style" commun à toutes les classes immuables du projet.
 */
@Value.Style(
        jakarta = true,
        // Le builder aura une méthode "fromInstance" qui permet de l'initialiser à partir d'une autre instance. La
        // méthode "from" est masquée par FisholaMail#from donc on choisit d'introduire un nouveau nom par défaut
        from = "fromInstance")
public @interface ImmutableObject {

}
