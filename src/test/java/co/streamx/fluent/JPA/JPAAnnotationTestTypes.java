package co.streamx.fluent.JPA;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import javax.persistence.Table;

import co.streamx.fluent.notation.Tuple;
import lombok.Data;
import lombok.EqualsAndHashCode;

public interface JPAAnnotationTestTypes {
    @Entity
    @Inheritance(strategy = InheritanceType.SINGLE_TABLE)
    @DiscriminatorColumn(name = "CHILD_TYPE", length = 1)
    @Table(name = "MEMBERS", schema = "mtm")
    @Data
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    public abstract class GroupMember {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "id")
        private Integer id;

        @ManyToMany
        @JoinTable(name = "GROUP_MEMBER", schema = "mtm", joinColumns = @JoinColumn(name = "MEMBER_ID", referencedColumnName = "ID"), inverseJoinColumns = @JoinColumn(name = "PARENT_ID", referencedColumnName = "ID"))
        private Set<Group> parents = new HashSet<>();

        public abstract boolean isLeaf();
    }

    @Entity
    @DiscriminatorValue("G")
    @Data
    @EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
    class Group extends GroupMember {

        @ManyToMany(mappedBy = "parents")
        private Set<GroupMember> members = new HashSet<>();

        public boolean isLeaf() {
            return false;
        }

    }

    @Entity
    @DiscriminatorValue("U")
    @SecondaryTable(name = "USERS", schema = "mtm", pkJoinColumns = @PrimaryKeyJoinColumn(name = "UID"))
    @Data
    @EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
    class User extends GroupMember {

        @EqualsAndHashCode.Include
        @Column(table = "USERS")
        private String name;

        public boolean isLeaf() {
            return true;
        }

    }

    @Table(name = "EMP", schema = "TPC")
    @Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
    @Tuple
    @Data
    public class Employee {
        @Id
        @GeneratedValue
        private long id;
        private String name;
    }

    @Tuple
    @Table(name = "FULL_TIME_EMP", schema = "TPC")
    @Data
    @EqualsAndHashCode(callSuper = true)
    public class FullTimeEmployee extends Employee {
        private int salary;
    }

    @Inheritance(strategy = InheritanceType.JOINED)
    @Tuple
    @Data
    @DiscriminatorColumn(name = "EMP_TYPE")
    @Table(name = "EMP")
    public class Employee1 {
        @Id
        @GeneratedValue
        private long id;
        private String name;
    }

    @Tuple
    @Table(name = "FULL_TIME_EMP", schema = "JN")
    @Data
    @EqualsAndHashCode(callSuper = true)
    @DiscriminatorValue("F")
    public class FullTimeEmployee1 extends Employee1 {
        private int salary;
    }
}
