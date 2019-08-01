package co.streamx.fluent.JPA;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import co.streamx.fluent.JPA.repository.NetworkObjectRepo;

public class NetworkObjectRepoTest extends IntegrationTest {

    @Autowired
    private NetworkObjectRepo networkObjectRepo;

    @Test
    @Transactional
    public void testBasicNativeQuery() {
        assertThat(networkObjectRepo.getAllNative().size()).isEqualTo(0);
    }

    @Test
    @Transactional
    public void testFindInRange() {
        assertThat(networkObjectRepo
                .findInRange(Arrays.asList("a", "b"), BigInteger.valueOf(3), Arrays.asList(1, 2), 4L, 5L)
                .size()).isEqualTo(0);
    }
}
