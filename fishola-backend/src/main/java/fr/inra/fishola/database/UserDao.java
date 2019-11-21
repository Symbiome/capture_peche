package fr.inra.fishola.database;

import at.favre.lib.crypto.bcrypt.BCrypt;
import fr.inra.fishola.entities.tables.records.FisholaUserRecord;

import javax.inject.Singleton;

import java.util.Optional;

import static fr.inra.fishola.entities.Tables.FISHOLA_USER;

@Singleton
public class UserDao extends AbstractFisholaDao {

    public boolean authenticate(String email, String password) {

        return run(context -> {
            try {
                FisholaUserRecord user = context.selectFrom(FISHOLA_USER)
                        .where(FISHOLA_USER.EMAIL.equal(email))
                        .fetchSingle();

                BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), user.getPassword());
                return result.verified;
            } catch (Exception eee) {
                eee.printStackTrace();
            }
            return false;
        });
    }

    protected UserProfile toUserProfile(FisholaUserRecord record) {
        ImmutableUserProfile result = ImmutableUserProfile.builder()
                .email(record.getEmail())
                .firstName(record.getFirstName())
                .lastName(Optional.ofNullable(record.getLastName()))
                .birthYear(Optional.ofNullable(record.getBirthYear()))
                .gender(Optional.ofNullable(record.getGender()))
                .build();
        return result;
    }

    public Optional<UserProfile> loadUser(String email) {

        return run(context -> {
            FisholaUserRecord user = context.selectFrom(FISHOLA_USER)
                    .where(FISHOLA_USER.EMAIL.equal(email))
                    .fetchAny();

            Optional<UserProfile> result = Optional.ofNullable(user)
                    .map(this::toUserProfile);
            return result;
        });

    }

}
