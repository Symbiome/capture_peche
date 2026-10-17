package fr.inrae.fishola.rest.security.admin;

/*-
 * #%L
 * Carnet de pêche :: Backend
 * %%
 * Copyright (C) 2019 - 2026 INRAE - UMR CARRTEL
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import fr.inrae.fishola.database.AdminDao;
import fr.inrae.fishola.database.UsersDao;
import fr.inrae.fishola.rest.department.Departments;
import fr.inrae.fishola.rest.imports.CsvSupport;
import fr.inrae.fishola.rest.security.AccountRules;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Crée les comptes des bêta-testeurs depuis un CSV (#192), une ligne par
 * compte : un même e-mail peut donner un compte pêcheur et un compte staff.
 * Rejouable : un compte qui existe déjà pour le couple (e-mail, type de compte)
 * est ignoré, jamais modifié. Les mots de passe du fichier sont hachés
 * (bcrypt) avant stockage.
 */
@Singleton
public class AccountImportService {

    static final List<String> EXPECTED_COLUMNS = List.of("email", "prenom", "nom", "profil", "perimetre", "mot_de_passe");

    private static final String PROFILE_VALUES = Arrays.stream(AccountProfile.values())
            .map(AccountProfile::csvValue)
            .collect(Collectors.joining(", "));

    @Inject
    protected UsersDao usersDao;

    @Inject
    protected AdminDao adminDao;

    private record AccountLine(String email, AccountProfile profile, Set<String> departments,
                               String firstName, String lastName, String password) {}

    /**
     * Importe les comptes d'un fichier CSV (séparateur « ; »).
     *
     * @param fileBytes contenu du fichier, UTF-8 ou Windows-1252
     * @return le rapport ligne par ligne, ou une erreur de fichier
     */
    public AccountImportReport run(byte[] fileBytes) {
        CsvSupport.Parsed parsed = CsvSupport.readRows(CsvSupport.decode(fileBytes));
        Set<String> header = parsed.header().stream().map(AccountImportService::normalizeColumn).collect(Collectors.toSet());
        List<String> missing = EXPECTED_COLUMNS.stream().filter(column -> !header.contains(column)).toList();
        if (!missing.isEmpty()) {
            return AccountImportReport.fileError("Colonnes manquantes : %s (en-tête attendu : %s)"
                    .formatted(String.join(", ", missing), String.join(";", EXPECTED_COLUMNS)));
        }
        AccountImportReport report = new AccountImportReport();
        List<Map<String, String>> records = parsed.records();
        for (int index = 0; index < records.size(); index++) {
            report.add(importRecord(index + 2, normalizeKeys(records.get(index))));
        }
        return report;
    }

    private AccountImportLine importRecord(int line, Map<String, String> record) {
        String email = StringUtils.trimToEmpty(record.get("email")).toLowerCase();
        Optional<AccountProfile> profile = AccountProfile.fromCsv(record.get("profil"));
        String profileLabel = profile.map(AccountProfile::csvValue).orElse(StringUtils.trimToEmpty(record.get("profil")));
        Set<String> departments = parseDepartments(record.get("perimetre"));
        Optional<String> error = validate(record, email, profile, departments);
        if (error.isPresent()) {
            return new AccountImportLine(line, email, profileLabel, AccountImportLine.REJECTED, error.get());
        }
        AccountLine account = new AccountLine(email, profile.get(), departments,
                StringUtils.trimToEmpty(record.get("prenom")), StringUtils.trimToNull(record.get("nom")),
                record.get("mot_de_passe"));
        String accountType = account.profile().isStaff() ? "back-office" : "pêcheur";
        if (exists(account)) {
            return new AccountImportLine(line, email, profileLabel, AccountImportLine.IGNORED,
                    "Compte %s déjà existant : non modifié".formatted(accountType));
        }
        create(account);
        return new AccountImportLine(line, email, profileLabel, AccountImportLine.CREATED,
                "Compte %s créé".formatted(accountType));
    }

    private static Optional<String> validate(Map<String, String> record, String email,
                                             Optional<AccountProfile> profile, Set<String> departments) {
        if (email.isEmpty()) {
            return Optional.of("E-mail obligatoire");
        }
        if (!AccountRules.isValidEmail(email)) {
            return Optional.of("E-mail invalide");
        }
        if (profile.isEmpty()) {
            return Optional.of("Profil inconnu « %s » (valeurs possibles : %s)"
                    .formatted(StringUtils.trimToEmpty(record.get("profil")), PROFILE_VALUES));
        }
        if (!profile.get().isStaff() && StringUtils.isBlank(record.get("prenom"))) {
            return Optional.of("Prénom obligatoire pour un compte pêcheur");
        }
        Optional<String> perimeterError = perimeterError(profile.get(), departments);
        if (perimeterError.isPresent()) {
            return perimeterError;
        }
        return AccountRules.passwordError(record.get("mot_de_passe"));
    }

    private static Optional<String> perimeterError(AccountProfile profile, Set<String> departments) {
        if (profile.isPerimeterRequired() && departments.isEmpty()) {
            return Optional.of("Périmètre obligatoire pour le profil %s : codes département séparés par des virgules"
                    .formatted(profile.csvValue()));
        }
        if (!profile.isPerimeterRequired() && !departments.isEmpty()) {
            return Optional.of("Pas de périmètre pour le profil %s".formatted(profile.csvValue()));
        }
        return departments.stream()
                .filter(code -> !Departments.isValidCode(code))
                .findFirst()
                .map(code -> "Code département inconnu : %s".formatted(code));
    }

    private boolean exists(AccountLine account) {
        if (account.profile().isStaff()) {
            return adminDao.findByEmail(account.email()).isPresent();
        }
        return usersDao.findByEmail(account.email()).isPresent();
    }

    /**
     * Crée le compte. Seul l'administrateur national gère les comptes staff
     * ({@code can_create_admin}) ; un administrateur régional peut recevoir ce
     * droit ensuite depuis l'écran « Administrateurs ».
     */
    private void create(AccountLine account) {
        AccountProfile profile = account.profile();
        if (profile.isStaff()) {
            adminDao.create(account.email(), adminDao.hashPassword(account.password()), profile.isNationalAdmin(),
                    profile.isNationalAdmin(), profile.isOperator(), account.departments().toArray(String[]::new));
        } else {
            usersDao.create(account.firstName(), account.lastName(),
                    uniquePseudo(account.firstName(), account.lastName()), account.email(),
                    usersDao.hashPassword(account.password()), false, null, null);
        }
    }

    /**
     * Pseudo par défaut d'un pêcheur importé : prénom et initiale du nom,
     * suffixé d'un numéro s'il est déjà pris. Le pêcheur peut le changer
     * ensuite dans son profil.
     */
    private String uniquePseudo(String firstName, String lastName) {
        String base = lastName == null ? firstName : "%s %s.".formatted(firstName, lastName.charAt(0));
        String candidate = base;
        for (int suffix = 2; !usersDao.findByPseudo(candidate).isEmpty(); suffix++) {
            candidate = "%s %d".formatted(base, suffix);
        }
        return candidate;
    }

    /**
     * Lit la colonne « perimetre » : codes département séparés par des
     * virgules ou des espaces. Un code à un chiffre (zéro initial perdu par
     * Excel) est complété : « 1 » devient « 01 ».
     */
    static Set<String> parseDepartments(String value) {
        return Arrays.stream(StringUtils.trimToEmpty(value).split("[,|\\s]+"))
                .filter(code -> !code.isEmpty())
                .map(code -> code.toUpperCase().matches("\\d") ? "0" + code : code.toUpperCase())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Map<String, String> normalizeKeys(Map<String, String> record) {
        Map<String, String> result = new LinkedHashMap<>();
        record.forEach((key, value) -> result.put(normalizeColumn(key), value));
        return result;
    }

    /** « Mot de passe », « Prénom » ou « PERIMETRE » valent « mot_de_passe », « prenom », « perimetre ». */
    private static String normalizeColumn(String column) {
        return StringUtils.stripAccents(StringUtils.trimToEmpty(column)).toLowerCase().replaceAll("[\\s-]+", "_");
    }
}
