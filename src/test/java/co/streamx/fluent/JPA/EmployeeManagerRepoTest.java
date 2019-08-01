package co.streamx.fluent.JPA;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import co.streamx.fluent.JPA.repository.EmployeeManagerRepo;
import co.streamx.fluent.JPA.repository.EmployeeManagerRepo.EmployeeManagerPair;

public class EmployeeManagerRepoTest extends IntegrationTest {
    @Autowired
    private EmployeeManagerRepo employeeRepository;

    @Test
    @Transactional()
    public void test1() {
        List<EmployeeManagerPair> eManagers = employeeRepository.getEManagers();
        assertThat(eManagers.size()).isEqualTo(7);
        assertThat(eManagers.stream().map(EmployeeManagerPair::getManager).distinct().count()).isEqualTo(3);
    }
}
