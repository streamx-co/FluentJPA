package co.streamx.fluent.JPA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import co.streamx.fluent.JPA.repository.CompanyRepository;
import co.streamx.fluent.JPA.repository.CourseRepository;
import co.streamx.fluent.JPA.repository.EmployeeRepository;
import co.streamx.fluent.JPA.repository.PhoneRepository;
import co.streamx.fluent.JPA.repository.StudentRepository;
import co.streamx.fluent.JPA.repository.entities.Company;
import co.streamx.fluent.JPA.repository.entities.Course;
import co.streamx.fluent.JPA.repository.entities.Employee;
import co.streamx.fluent.JPA.repository.entities.EmployeeId;
import co.streamx.fluent.JPA.repository.entities.Phone;
import co.streamx.fluent.JPA.repository.entities.Student;

public class EmployeeRepositoryTest extends IntegrationTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PhoneRepository phoneRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @PersistenceContext
    private EntityManager em;

    @Test
    @Transactional()
    public void test1() {

        Company company = new Company();
        company.setId(1L);
        company.setName("vladmihalcea.com");
        companyRepository.save(company);

        em.flush();
        em.clear();

        Employee employee = new Employee();
        EmployeeId employeeId = new EmployeeId(company, 100L);
        employee.setId(employeeId);
        employee.setName("Vlad Mihalcea");

        employeeRepository.save(employee);

        em.flush();
        em.clear();

        employee = employeeRepository.findById(new EmployeeId(company, 100L)).get();
        Phone phone = new Phone();
        phone.setEmployee(employee);
        phone.setNumber("012-345-6789");
        phoneRepository.save(phone);

        em.flush();
        em.clear();

        phone = phoneRepository.findById("012-345-6789").get();
        assertNotNull(phone);
        assertEquals(new EmployeeId(company, 100L), phone.getEmployee().getId());
        assertNotNull(phone.getEmployee().getId().getCompanyId());

        assertThat(employeeRepository.getAllNativeWhere("Vlad Mihalcea").size()).isEqualTo(1);
        assertThat(employeeRepository.getAllNative().size()).isEqualTo(1);
        assertThat(employeeRepository.getEmployeeCompanies().size()).isEqualTo(1);
        assertThat(employeeRepository.getEmployeeCompanies1().size()).isEqualTo(1);
        assertThat(employeeRepository.getEmployeeCompanies2().size()).isEqualTo(1);
    }

    @Test
    @Transactional()
    public void test2() {
        Student student1 = new Student();
        student1.setName("s1");
        Student student2 = new Student();
        student2.setName("s2");

        Course course1 = new Course();
        course1.setName("c1");

        student1.getLikedCourses().add(course1);
        student2.getLikedCourses().add(course1);

        course1.getLikes().add(student1);
        course1.getLikes().add(student2);

        courseRepository.save(course1);

        studentRepository.save(student1);
        studentRepository.save(student2);

        em.flush();
        em.clear();

        List<Course> foundCourses = courseRepository.findByName("c1");
        assertNotNull(foundCourses);
        assertEquals(1, foundCourses.size());
        assertEquals(2, foundCourses.get(0).getLikes().size());

        int nStudents = courseRepository.countStudents("c1");
        assertEquals(2, nStudents);

        nStudents = courseRepository.countStudents1("c1");
        assertEquals(2, nStudents);
    }
}
