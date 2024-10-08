package co.streamx.fluent.JPA;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class UserRepositoryTest extends IntegrationTest implements ElementCollectionTypes {

    @Autowired
    private UserRepository userRepository;

    @PersistenceContext
    private EntityManager em;

    @BeforeAll
    public static void init() {
        FluentJPA.setCapabilities(Collections.emptySet());
    }

    @Test
    @Transactional()
    public void test1() {

        User user = setup();

        assertThat(userRepository.countPhones(user)).isEqualTo(2);
    }

    private User setup() {
        // Insert a user with multiple phone numbers and addresses.
        Set<String> phoneNumbers = new HashSet<>();
        phoneNumbers.add("+91-9999999999");
        phoneNumbers.add("+91-9898989898");

        Set<Address> addresses = new HashSet<>();
        addresses.add(new Address("747", "Golf View Road", "Bangalore", "Karnataka", "India", "560008"));
        addresses.add(new Address("Plot No 44", "Electronic City", "Bangalore", "Karnataka", "India", "560001"));

        User user = new User("Rajeev Kumar Singh", "rajeev@callicoder.com", phoneNumbers, addresses);

        userRepository.save(user);

        em.flush();
        em.clear();
        return user;
    }
}
