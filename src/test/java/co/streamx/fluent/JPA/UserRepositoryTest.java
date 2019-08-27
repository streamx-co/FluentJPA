package co.streamx.fluent.JPA;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class UserRepositoryTest extends IntegrationTest implements ElementCollectionTypes {

    @Autowired
    private UserRepository userRepository;

    @PersistenceContext
    private EntityManager em;

    @Test
    @Transactional()
    @Disabled
    public void test1() {

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

        Optional<User> found = userRepository.findById(user.getId());

        System.out.println(found);

        userRepository.countPhones(user);
    }
}
