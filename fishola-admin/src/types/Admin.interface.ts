interface Admin {
    email: string;
    isNationalAdmin?: boolean | undefined;
    canCreateAdmins?: boolean | undefined;
    isOperator?: boolean | undefined;
    // Périmètre géographique : codes département INSEE (#159). Vide pour un national.
    departmentCodes?: string[] | undefined;
    // Mode bêta-testeurs (#192) : réglage d'inscription et import de comptes, production seulement.
    betaTestersMode?: boolean | undefined;
}
