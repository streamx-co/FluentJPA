package co.streamx.fluent.JPA;

import static co.streamx.fluent.SQL.Directives.discardSQL;
import static co.streamx.fluent.SQL.Library.COUNT;
import static co.streamx.fluent.SQL.SQL.FROM;
import static co.streamx.fluent.SQL.SQL.SELECT;
import static co.streamx.fluent.SQL.SQL.WHERE;

import org.springframework.data.jpa.repository.JpaRepository;

import co.streamx.fluent.JPA.repository.EntityManagerSupplier;
import co.streamx.fluent.SQL.ElementCollection;

public interface UserRepository extends ElementCollectionTypes, JpaRepository<ElementCollectionTypes.User, Long>,
        EntityManagerSupplier, CommonTest {

    default int countPhones(User x) {
        Long id = x.getId();
        FluentQuery query = FluentJPA.SQL((User user,
                                           ElementCollection<User, String> userPhones) -> {

            discardSQL(userPhones.join(user, User::getPhoneNumbers));

            SELECT(COUNT());
            FROM(userPhones);

            WHERE(userPhones.getOwner().getId() == id);

        });

        // @formatter:off
        String expected = "SELECT COUNT(*) " + 
                "FROM EC.user_phone_numbers t1 " + 
                "WHERE (t1.user_id = ?1)";
        // @formatter:on

        assertQuery(query, expected);

        Number number = (Number) query.createQuery(getEntityManager()).getSingleResult();
        return number.intValue();
    }

}
