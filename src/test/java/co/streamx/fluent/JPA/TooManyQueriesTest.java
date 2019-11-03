package co.streamx.fluent.JPA;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class TooManyQueriesTest extends IntegrationTest implements TooManyQueriesTypes {
    private static final String HASH = "hash";

    @Autowired
    private TooManyPersonRepository personRepository;

    @PersistenceContext
    private EntityManager em;

    @BeforeAll
    public static void init() {
        FluentJPA.setCapabilities(Collections.emptySet());
    }

    @Test
    @Transactional
    public void preparePersonRepo() {
        Person p = new Person();
        Password pass = new Password();
        pass.setPasswordHash(HASH);

        Role role1 = new Role();
        role1.setRoleType(RoleType.TYPE1);

        Role role2 = new Role();
        role2.setRoleType(RoleType.TYPE2);

        p.setPassword(pass);
        p.getRoles().add(role1);
        p.getRoles().add(role2);

        personRepository.save(p);

        em.flush();
        em.clear();

        Optional<Person> ofound = personRepository.findById(p.getId());
        assertThat(ofound).isNotEmpty();

        Person found = ofound.get();
        found.getRoles();

        em.flush();
        em.clear();

        List<Person> all = personRepository.findAll();
        assertThat(all).hasSize(1);

        found = all.get(0);
        found.getRoles();

        em.flush();
        em.clear();

        found = personRepository.findById1(p.getId());
        found.getRoles();
        pass = found.getPassword();
        pass.getId();
        assertThat(pass.getPasswordHash()).isEqualTo(HASH);
    }
}
