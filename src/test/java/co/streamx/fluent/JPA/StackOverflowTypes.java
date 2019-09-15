package co.streamx.fluent.JPA;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import co.streamx.fluent.notation.Tuple;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

public interface StackOverflowTypes {

    @Tuple
    @Table(name = "SECURITY_PERMISSION", schema = "BARBANETUSER")
    public class SecurityPermissionEntity {

        private int id;

        @Id
        @Column(name = "ID_PK")
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SecurityPermission_Sequence")
        @SequenceGenerator(name = "SecurityPermission_Sequence", sequenceName = "SECURITY_PERMISSION_SEQ", allocationSize = 1)
        public int getId() {
            return id;
        }
    }

    @Tuple
    @Table(name = "SECURITY_ROLE", schema = "BARBANETUSER")
    public class SecurityRoleEntity {
        private int id;
//        private RoleTypeEnum type;
        private boolean manageView;
        private String title;
        private String slug;
        private Integer sortOrder;
        private boolean enabled;
        private boolean deleted;

        private Set<SecurityPermissionEntity> permissions;

        @Id
        @Column(name = "ID_PK")
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SecurityRole_Sequence")
        @SequenceGenerator(name = "SecurityRole_Sequence", sequenceName = "SECURITY_ROLE_SEQ", allocationSize = 1)
        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

//        @Basic
//        @Column(name = "TYPE_ID_FK")
//        @Convert(converter = RoleTypeConverter.class)
//        public RoleTypeEnum getType() {
//            return type;
//        }
//
//        public void setType(RoleTypeEnum type) {
//            this.type = type;
//        }

        @Basic
        @Column(name = "MANAGE_VIEW")
        public boolean isManageView() {
            return manageView;
        }

        public void setManageView(boolean manageView) {
            this.manageView = manageView;
        }

        @Basic
        @Column(name = "TITLE")
        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        @Basic
        @Column(name = "SLUG")
        public String getSlug() {
            return slug;
        }

        public void setSlug(String slug) {
            this.slug = slug;
        }

        @Basic
        @Column(name = "SORT_ORDER")
        public Integer getSortOrder() {
            return sortOrder;
        }

        public void setSortOrder(Integer sortOrder) {
            this.sortOrder = sortOrder;
        }

        @Basic
        @Column(name = "ENABLED")
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @Basic
        @Column(name = "DELETED")
        public boolean isDeleted() {
            return deleted;
        }

        public void setDeleted(boolean deleted) {
            this.deleted = deleted;
        }

        @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
        @JoinTable(name = "SECURITY_ROLE_PERMISSION", schema = "BARBANETUSER", joinColumns = @JoinColumn(name = "ROLE_ID_FK", referencedColumnName = "ID_PK", nullable = false), inverseJoinColumns = @JoinColumn(name = "PERMISSION_ID_FK", referencedColumnName = "ID_PK", nullable = false))
        public Set<SecurityPermissionEntity> getPermissions() {
            return permissions;
        }

        public void setPermissions(Set<SecurityPermissionEntity> permissions) {
            this.permissions = permissions;
        }
    }

    @Tuple
    @Table(name = "SECURITY_USER_REALM_ROLE", schema = "BARBANETUSER")
    public class SecurityUserRealmRoleEntity {
        private int id;
        private int userId;
        private int realmId;
        private int roleId;

//        private UserPersonEntity user;
//        private SecurityRealmEntity realm;
        private SecurityRoleEntity role;

        @Id
        @Column(name = "ID_PK")
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SecurityUserRealmRole_Sequence")
        @SequenceGenerator(name = "SecurityUserRealmRole_Sequence", sequenceName = "SECURITY_USER_REALM_ROLE_SEQ", allocationSize = 1)
        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        @Basic
        @Column(name = "USER_ID_FK")
        public int getUserId() {
            return userId;
        }

        public void setUserId(int userId) {
            this.userId = userId;
        }

        @Basic
        @Column(name = "REALM_ID_FK")
        public int getRealmId() {
            return realmId;
        }

        public void setRealmId(int realmId) {
            this.realmId = realmId;
        }

        @Basic
        @Column(name = "ROLE_ID_FK")
        public int getRoleId() {
            return roleId;
        }

        public void setRoleId(int roleId) {
            this.roleId = roleId;
        }

//        @ManyToOne(fetch = FetchType.LAZY)
//        @JoinColumn(name = "USER_ID_FK", referencedColumnName = "ID_PK", insertable = false, updatable = false)
//        public UserPersonEntity getUser() {
//            return user;
//        }
//
//        public void setUser(UserPersonEntity user) {
//            this.user = user;
//        }
//
//        @ManyToOne(fetch = FetchType.LAZY)
//        @JoinColumn(name = "REALM_ID_FK", referencedColumnName = "ID_PK", insertable = false, updatable = false)
//        public SecurityRealmEntity getRealm() {
//            return realm;
//        }
//
//        public void setRealm(SecurityRealmEntity realm) {
//            this.realm = realm;
//        }

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "ROLE_ID_FK", referencedColumnName = "ID_PK", insertable = false, updatable = false)
        public SecurityRoleEntity getRole() {
            return role;
        }

        public void setRole(SecurityRoleEntity role) {
            this.role = role;
        }
    }

    @Tuple
    @Getter
    @Table(name = "USER")
    class User {
        private Long id;
        private String name;
    }

    @Tuple
    @Getter
    @Table(name = "USER_LOG")
    class UserLog {
        private Long userId;
    }

    @Tuple
    @Getter
    class UserIdCount {
        private Long userId;
        private int count;
    }

    @Tuple
    @Data
    class UserNameCount {
        private int count;
        private String name;
    }

    @Tuple
    @Data
    class Movie {
        @Id
        String id;

        @ManyToMany
        @JoinTable(name = "movie_category", joinColumns = @JoinColumn(name = "movie_id", referencedColumnName = "id"))
        Set<Category> categories = new HashSet<>();
    }

    @Tuple
    @Data
    @Table(name = "CATEGORIES")
    class Category {

        @Id
        String id;

    }

    @Tuple
    @Data
    @Table(name = "STUDENTS")
    class Student {

        @Id
        String id;

        String name;

    }

    @Tuple
    @Data
    @Table(name = "SUBJECTS")
    class Subject {

        @Id
        String id;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "student_id")
        Student student;

        String title;

        int marks;

    }

    @Tuple
    @Table(name = "tbl_error_type")
    @Data
    class ErrorType {

        @Id
        private int id;

        @OrderBy(value = "createAt DESC")
        @OneToMany(mappedBy = "errorType", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//        @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)    
        private Set<ErrorRecord> errors = new LinkedHashSet<>();

        @ManyToOne
        @JoinColumn(name = "device_id")
        private Device device;
    }

    @Tuple
    @Table(name = "tbl_error_record")
    @Data
    @EqualsAndHashCode(callSuper = true)
    class ErrorRecord extends ErrorContent {

        @Id
        private int id;

        @ManyToOne
        @JoinColumn(name = "error_type_id")
        private ErrorType errorType;

        private Timestamp createdAt;
    }

    @Embeddable
    @MappedSuperclass
    @Data
    class ErrorContent {
        private String errorDescription;
    }

    @Tuple
    @Table(name = "tbl_device")
    @Data
    class Device {
        @Id
        private int id;
    }

    @Tuple
    @Table(name = "INFLUENCE_DETAILS")
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public class InfluenceDetails {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "influence_summary_id", length = 64)
        private Long influenceSummaryId;

        @OneToOne(fetch = FetchType.EAGER)
        @JoinColumn(name = "aggregator_id", nullable = false, referencedColumnName = "agg_id", updatable = false, insertable = false)
        private QualityAggregator aggregators;

        @Column(name = "rule_id", length = 64)
        private String ruleId;

        @Column(name = "rule_desc", nullable = false)
        private String ruleDesc;

        @Column(name = "value")
        private String value;
    }

    @Tuple
    @Table(name = "QUALITY_AGGREGATOR")
    @AllArgsConstructor
    @NoArgsConstructor
    @Accessors(chain = true)
    @Builder
    @Data
    public class QualityAggregator {

        @Id
        @Column(name = "agg_id")
        private String aggId;

        @Column(name = "project_id")
        private String projectId;

        @Column(name = "job_id")
        private String jobId;

        @Column(name = "job_instance_id")
        private String jobInstanceId;

        @Column(name = "created_at")
        private Date timestamp;

        @Column(name = "agg_key")
        private String aggKey;

        @Column(name = "agg_value")
        private String aggValue;

        @Column(name = "level")
        private Integer level;

        @Column(name = "parent_agg_id")
        private String parentAggId;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "parent_agg_id", referencedColumnName = "agg_id", insertable = false, updatable = false)
        private QualityAggregator parent;

        @OneToMany(mappedBy = "parent", fetch = FetchType.EAGER)
        private Set<QualityAggregator> children;
    }

    @Tuple
    @Table(name = "product")
    public class Product {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "id")
        private Integer id;

        @OneToMany(fetch = FetchType.LAZY, mappedBy = "product")
        private Set<OrderProduct> orderProductList = new LinkedHashSet<>();

        // ...

    }

    @Tuple
    @Table(name = "order_product")
    public class OrderProduct {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "id")
        private Integer id;

        @ManyToOne(fetch = FetchType.EAGER)
        @JoinColumn(name = "product_id")
        private Product product;

        // ...

    }

    @Tuple
    @Table(name = "tab1")
    @Data
    class Table1 {
        @Id
        private Integer id;
        long projectId;
        long empId;
    }

    @Tuple
    @Table(name = "tab2")
    @Data
    class Table2 {
        @Id
        private Integer id;
        long bpId;
        @Column(name = "projectName")
        String projectName;
    }

    @Tuple
    @Table(name = "tab3")
    @Data
    class Table3 {
        @Id
        private Integer id;
        @Column(name = "empName")
        String empName;
        String contactNum;
    }

    @Tuple
    @Data
    public class Person {
        @Id
        private Long id;
        private String firstName;
        private String lastName;
        private int age;
    }

    @Data // lombok annotation to create constructor, equals and hash-code
    public class PersonDTO {
        private String firstName;
        private String lastName;
    }

    @Tuple
    @Data
    public class BrandMerchant {
        @Id
        private Long id;
        private Table3 brand;
        private Table3 category;
        private Table3 merchant;
    }
}
