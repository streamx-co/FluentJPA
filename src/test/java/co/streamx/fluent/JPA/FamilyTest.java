package co.streamx.fluent.JPA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import co.streamx.fluent.JPA.repository.ChildRepository;
import co.streamx.fluent.JPA.repository.ParentRepository;
import co.streamx.fluent.JPA.repository.entities.Child;
import co.streamx.fluent.JPA.repository.entities.Parent;

public class FamilyTest extends IntegrationTest {

    @Autowired
    private ParentRepository parentRepo;

    @Autowired
    private ChildRepository childRepo;

    @PersistenceContext
    private EntityManager em;

    @Test
    @Transactional
    public void test1() {
        Parent p = new Parent();
        p.setId(1);
        p.setCode("S");

        Child c1 = new Child();
        c1.setId(p.getId());
        c1.setCode(p.getCode());
        c1.setIndex(1);
        c1.setParent(p);

        Child c2 = new Child();
        c2.setId(p.getId());
        c2.setCode(p.getCode());
        c2.setIndex(2);
        c2.setParent(p);

        p.getChilds().add(c1);
        p.getChilds().add(c2);

        parentRepo.save(p);

        em.flush();
        em.clear();

        long count = childRepo.count();

        assertThat(count).isEqualTo(2);

        assertThat(parentRepo.findById(new Parent.Key("S", 1))).isNotNull();

        List<Child> children = parentRepo.getParentChildrenJPQL();

        em.clear();

        children = parentRepo.getParentChildren();

        assertThat(children.size()).isEqualTo(2);

        em.clear();

        children = parentRepo.getParentChildren2();

        assertThat(children.size()).isEqualTo(2);

        assertThat(childRepo.findById(new Child.Key("S", 1, 2))).isNotNull();

        String[] expected = { "S", "S" }; // 2 children

        assertArrayEquals(expected, parentRepo.getParentCodes().toArray());
    }
}
