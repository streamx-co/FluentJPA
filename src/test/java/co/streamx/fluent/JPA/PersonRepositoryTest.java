package co.streamx.fluent.JPA;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import co.streamx.fluent.JPA.repository.PersonRepository;

public class PersonRepositoryTest extends IntegrationTest {
    @Autowired
    private PersonRepository personRepository;

    @BeforeAll
    public static void init() {
        FluentJPA.setCapabilities(Collections.emptySet());
    }

    @Test
    @Transactional
    public void testPersonRepo() {
        assertThat(personRepository.count()).isEqualTo(1);
    }

    @Test
    @Transactional
    public void testEntityManagerProviderIntegration() {
        assertThat(personRepository.getAll().size()).isEqualTo(1);
    }

    @Test
    @Transactional
    public void testBasicNativeQuery() {
        assertThat(personRepository.getAllNative().size()).isEqualTo(1);
    }

    @Test
    @Transactional
    public void testPassArguments() {
        assertThat(personRepository.getAllByName("Dave").size()).isEqualTo(1);
    }

    @Test
    @Transactional
    public void testInsertDefault() {
        assertThat(personRepository.insertDefault("John", 4).getId()).isNotNull();
    }
}
