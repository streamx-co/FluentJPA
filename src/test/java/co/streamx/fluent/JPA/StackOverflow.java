package co.streamx.fluent.JPA;

import static co.streamx.fluent.SQL.AggregateFunctions.COUNT;
import static co.streamx.fluent.SQL.AggregateFunctions.ROW_NUMBER;
import static co.streamx.fluent.SQL.AggregateFunctions.SUM;
import static co.streamx.fluent.SQL.Directives.aggregateBy;
import static co.streamx.fluent.SQL.Directives.alias;
import static co.streamx.fluent.SQL.Directives.injectSQL;
import static co.streamx.fluent.SQL.Directives.parameter;
import static co.streamx.fluent.SQL.Directives.subQuery;
import static co.streamx.fluent.SQL.Library.pick;
import static co.streamx.fluent.SQL.Library.selectAll;
import static co.streamx.fluent.SQL.MySQL.SQL.GROUP_CONCAT;
import static co.streamx.fluent.SQL.MySQL.SQL.LIMIT;
import static co.streamx.fluent.SQL.MySQL.SQL.STR_TO_DATE;
import static co.streamx.fluent.SQL.Operators.BETWEEN;
import static co.streamx.fluent.SQL.Operators.lessEqual;
import static co.streamx.fluent.SQL.Oracle.SQL.TO_DATE;
import static co.streamx.fluent.SQL.Oracle.SQL.registerVendorCapabilities;
import static co.streamx.fluent.SQL.SQL.BY;
import static co.streamx.fluent.SQL.SQL.DISTINCT;
import static co.streamx.fluent.SQL.SQL.FROM;
import static co.streamx.fluent.SQL.SQL.GROUP;
import static co.streamx.fluent.SQL.SQL.ORDER;
import static co.streamx.fluent.SQL.SQL.PARTITION;
import static co.streamx.fluent.SQL.SQL.SELECT;
import static co.streamx.fluent.SQL.SQL.UPDATE;
import static co.streamx.fluent.SQL.SQL.WHERE;
import static co.streamx.fluent.SQL.SQL.WITH;
import static co.streamx.fluent.SQL.ScalarFunctions.CASE;
import static co.streamx.fluent.SQL.ScalarFunctions.CURRENT_DATE;
import static co.streamx.fluent.SQL.ScalarFunctions.WHEN;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.persistence.AttributeOverride;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import co.streamx.fluent.SQL.JoinTable;
import co.streamx.fluent.SQL.Oracle.Format;
import co.streamx.fluent.SQL.Oracle.FormatModel;
import co.streamx.fluent.functions.Function1;
import co.streamx.fluent.notation.Keyword;
import co.streamx.fluent.notation.Local;
import co.streamx.fluent.notation.Tuple;
import lombok.Data;
import lombok.Getter;

public class StackOverflow implements CommonTest, StackOverflowTypes {

    @BeforeAll
    public static void init() {
        registerVendorCapabilities(FluentJPA::setCapabilities);
    }

    private EntityManager em;

    @Entity
    @Data
    public static class Location {
        @Id
        private int id;
        private String location;

        @ManyToOne
        @JoinColumn(name = "label_id")
        private Translation translation;
    }

    @Entity
    @Data
    public static class Translation {
        @Id
        private int id;
        private String english;
        private String german;
    }

    @Tuple
    @Getter
    public static class TranslatedLocation {
        @Id
        private int id;
        private String location;
        private String label;
    }

    // https://stackoverflow.com/questions/57361456/java-persistence-join-different-columns-dependent-on-runtime-parameter
    @Test
    public void differentColumns() {
        int langCode = 1;
        FluentQuery query = FluentJPA.SQL((Location l,
                                           Translation t) -> {

            String trans = CASE(WHEN(langCode == 1).THEN(t.getEnglish()).ELSE(t.getGerman())).END();
            String label = alias(trans, TranslatedLocation::getLabel);

            SELECT(l.getId(), l.getLocation(), label);
            FROM(l).JOIN(t).ON(l.getTranslation() == t);

        });

        String expected = "SELECT t0.id, t0.location, CASE WHEN (?1 = 1)  THEN t1.english  ELSE t1.german   END   AS label "
                + "FROM Location t0  INNER JOIN Translation t1  ON (t0.label_id = t1.id)";

        assertQuery(query, expected);
    }

    public List<TranslatedLocation> getTranslatedLocations(int langCode) {
        FluentQuery query = FluentJPA.SQL((Location l,
                                           Translation t) -> {

            String trans = CASE(WHEN(langCode == 1).THEN(t.getEnglish()).ELSE(t.getGerman())).END();
            String label = alias(trans, TranslatedLocation::getLabel);

            SELECT(l.getId(), l.getLocation(), label);
            FROM(l).JOIN(t).ON(l.getTranslation() == t);

        });
        query.createQuery(em).executeUpdate();
        return query.createQuery(em, TranslatedLocation.class).getResultList();
    }

    @Entity
    @Data
    @Table(name = "PriceTags")
    public static class PriceTag {
        @Id
        private int id;
        private int goodsId;
        private int price;
        private Date updatedDate;
    }

    @Tuple
    @Getter
    public static class RankedPriceTag extends PriceTag {
        private long rowNumber;
    }

    @Test
    // https://stackoverflow.com/questions/57351634/is-there-a-way-to-get-the-latest-entry-that-is-less-or-equal-to-the-current-time
    public void latestEntry() {

        Keyword joinedDecls = Keyword.join(declareNumberToBoolean(), declareNumberToBoolean());

        FluentQuery query = FluentJPA.SQL(() -> {

            RankedPriceTag ranked = subQuery((PriceTag tag) -> {
                Long rowNumber = alias(
                        aggregateBy(ROW_NUMBER())
                                .OVER(PARTITION(BY(tag.getGoodsId())).ORDER(BY(tag.getUpdatedDate()).DESC())),
                        RankedPriceTag::getRowNumber);

                SELECT(tag, rowNumber);
                FROM(tag);
            });

            WITH(joinedDecls, ranked);
            selectAll(ranked);
            WHERE(lessEqual(ranked.getUpdatedDate(), CURRENT_DATE()) && ranked.getRowNumber() == 1);

        });

        String expected = "WITH " + declareNumberToBoolean() + declareNumberToBoolean() + " q0 AS "
                + "(SELECT t0.*,  ROW_NUMBER()  OVER(PARTITION BY  t0.goods_id   ORDER BY  t0.updated_date  DESC   ) AS row_number "
                + "FROM PriceTags t0 ) " + "SELECT q0.* " + "FROM q0 "
                + "WHERE ((q0.updated_date <= CURRENT_DATE   ) AND (q0.row_number = 1))";

        assertQuery(query, expected);
    }

    @Local
    public static Keyword declareNumberToBoolean() {
        // @formatter:off
        return injectSQL("FUNCTION number_to_boolean_(i NUMBER)" + 
                "  RETURN NUMBER" + 
                "  IS" + 
                "    b BOOLEAN;" + 
                "  BEGIN" + 
                "    -- Actual function call" + 
                "    b := number_to_boolean(i);" + 
                "     " + 
                "    -- Translation to numeric result" + 
                "    RETURN CASE b WHEN TRUE THEN 1 WHEN FALSE THEN 0 END;" + 
                "  END number_to_boolean_;");
        // @formatter:on
    }

    @Entity
    @Data // lombok
    public static class PymtEntity {
        @Id
        private int id;
        private int feeAmt;
        private int tranAmt;
        private Timestamp tranCaptrTm;
        private String tranTypeCde;
        private int payeeId;
        private String respCde;
    }

    @Test
    // https://stackoverflow.com/questions/57311620/create-hibernate-query-to-perform-sum-of-two-select-query-columns
    public void sumTwoColumns() {

        Date from = null;
        Date to = null;
        int md = 0;
        String re = null;

        FluentQuery query = FluentJPA.SQL(() -> {
            PymtEntity common = subQuery((PymtEntity pymt) -> {
                SELECT(pymt);
                FROM(pymt);
                WHERE(BETWEEN(pymt.getTranCaptrTm(), from, to) && pymt.getPayeeId() == md && pymt.getRespCde() == re);
            });

            PymtEntity first = subQuery(() -> {
                SELECT(common);
                FROM(common);
                WHERE(common.getTranTypeCde() == "003");
            });

            PymtEntity second = subQuery(() -> {
                SELECT(common);
                FROM(common);
                WHERE(common.getTranTypeCde() != "003");
            });

            SELECT(SUM(pick(first, SUM(first.getFeeAmt() + first.getTranAmt()))
                    - pick(second, SUM(second.getTranAmt() - second.getFeeAmt()))));
        });

        String expected = "SELECT SUM(((SELECT SUM((q3.fee_amt + q3.tran_amt)) " + "FROM (SELECT q2.* "
                + "FROM (SELECT t0.* " + "FROM PymtEntity t0 "
                + "WHERE ((t0.tran_captr_tm BETWEEN ?1 AND ?2 ) AND ((t0.payee_id = ?3) AND (t0.resp_cde = ?4))) ) q2 "
                + "WHERE (q2.tran_type_cde = '003') ) q3 ) - (SELECT SUM((q4.tran_amt - q4.fee_amt)) "
                + "FROM (SELECT q2.* " + "FROM (SELECT t0.* " + "FROM PymtEntity t0 "
                + "WHERE ((t0.tran_captr_tm BETWEEN ?1 AND ?2 ) AND ((t0.payee_id = ?3) AND (t0.resp_cde = ?4))) ) q2 "
                + "WHERE (q2.tran_type_cde <> '003') ) q4 )))";

        assertQuery(query, expected);
    }

    public int sumTwoColumns(Date from,
                             Date to,
                             int md,
                             String re) {
        FluentQuery query = FluentJPA.SQL(() -> {
            PymtEntity common = subQuery((PymtEntity pymt) -> {
                SELECT(pymt);
                FROM(pymt);
                WHERE(BETWEEN(pymt.getTranCaptrTm(), from, to) && pymt.getPayeeId() == md && pymt.getRespCde() == re);
            });

            PymtEntity first = subQuery(() -> {
                SELECT(common);
                FROM(common);
                WHERE(common.getTranTypeCde() == "003");
            });

            PymtEntity second = subQuery(() -> {
                SELECT(common);
                FROM(common);
                WHERE(common.getTranTypeCde() != "003");
            });

            SELECT(SUM(pick(first, SUM(first.getFeeAmt() + first.getTranAmt()))
                    - pick(second, SUM(second.getTranAmt() - second.getFeeAmt()))));
        });

        return query.createQuery(em, Integer.class).getSingleResult();
    }

    @Embeddable
    @Data
    public static class EmbeddedEntity {

        private String normalString2;
    }

    @Tuple
    @Table(name = "ENTITY_A")
    @Data
    public static class EntityA {

        @Embedded
        @AttributeOverride(name = "normalString2", column = @Column(name = "normal_string3"))
        private EmbeddedEntity embeddedEntity;
        @Column(name = "NORMAL_STRING")
        private String normalString;
    }

    @Test
    // https://stackoverflow.com/questions/57428383/how-to-update-an-embedded-entity-reference-inside-an-entity-with-jpql
    public void updateEmbedded() {

        String string1 = null;
        String string2 = null;

        FluentQuery query = updateEntity(string1, string2);

        String expected = "UPDATE ENTITY_A t0  SET NORMAL_STRING = ?1 " + "normal_string3 = ?2";

        assertQuery(query, expected);
    }

    private FluentQuery updateEntity(String string1,
                                     String string2) {
        FluentQuery query = FluentJPA.SQL((EntityA a) -> {
            UPDATE(a).SET(() -> {
                a.setNormalString(string1);
                a.getEmbeddedEntity().setNormalString2(parameter(string2));
            });
        });

//        query.createQuery(em).executeUpdate();
        return query;
    }

    @Entity
    @Table(name = "act_resource_t", schema = "sbill")
    @Data
    public static class ActResource {

        @Id
        private int id;

        private int currentBAL;
        private String createdDT;
    }

    @Tuple
    @Data
    public static class BalanceByDate {
        private Date date;
        private int balance;
    }

    @Test
    // https://stackoverflow.com/questions/57429484/common-functions-for-oracle-and-mysql
    public void commonFunction() {

        FluentQuery query = balanceByDate();

        String expected = "SELECT STR_TO_DATE(t0.created_dt, '%d-%m-%y') AS date, SUM(t0.current_bal) AS balance "
                + "FROM sbill.act_resource_t t0 " + "GROUP BY  STR_TO_DATE(t0.created_dt, '%d-%m-%y')";

        assertQuery(query, expected);
    }

    private FluentQuery balanceByDate() {
        FluentQuery query = FluentJPA.SQL((ActResource e) -> {

            Date createdDate = alias(AS_DATE().apply(e.getCreatedDT()), BalanceByDate::getDate);
            Integer balance = alias(SUM(e.getCurrentBAL()), BalanceByDate::getBalance);

            SELECT(createdDate, balance);
            FROM(e);
            GROUP(BY(createdDate));
        });

//        query.createQuery(em, BalanceByDate.class).getSingleResult();
        return query;
    }

    public static final FormatModel DD_MM_YY = Format.dateModel(Format.DD, Format.MM, Format.YY);

    public static boolean isOracle() {
        return false;
    }

    @Local
    public static Function1<String, Date> AS_DATE() {
        if (isOracle())
            return s -> TO_DATE(s, DD_MM_YY); // oracle

        return s -> STR_TO_DATE(s, "%d-%m-%y"); // mysql
    }

    @Entity
    @Data
    public class Widget {
        @Id
        private int id;

        @OneToMany(cascade = CascadeType.ALL, mappedBy = "widget")
        private List<Tag> tagList;
    }

    @Entity
    @Data
    public class Tag {
        @Id
        private int id;

        private String tagValue;

        @ManyToOne
        private Widget widget;

    }

    @Test
    // https://stackoverflow.com/questions/57452245/jpa-criteria-query-match-any-of-list-passed-as-parameter/
    public void matchAny() {

        String[] args = { "tag1", "tag2", "tag3" };
        List<String> tags = Arrays.asList(args);

        Function1<Tag, Boolean> dynamicFilter = buildAnd(tags);

        FluentQuery query = FluentJPA.SQL((Widget w,
                                           Tag tag) -> {

            SELECT(DISTINCT(w));
            FROM(w).JOIN(tag).ON(tag.getWidget() == w);
            WHERE(dynamicFilter.apply(tag));
        });

        String expected = "SELECT DISTINCT t0.*  " + "FROM Widget t0  INNER JOIN Tag t1  ON (t1.widget_id = t0.id) "
                + "WHERE (((t1.tag_value = ?1) AND (t1.tag_value = ?2)) AND (t1.tag_value = ?3))";

        assertQuery(query, expected, args);
    }

    private Function1<Tag, Boolean> buildAnd(List<String> tags) {
        Function1<Tag, Boolean> criteria = Function1.TRUE();

        for (String tag : tags)
            criteria = criteria.and(p -> p.getTagValue() == parameter(tag));

        return criteria;
    }

    @Entity
    @Data // lombok
    @Table(name = "profile")
    public class Profile {
        @Id
        private int id;

        private String description;
    }

    @Entity
    @Data // lombok
    @Table(name = "profile_menu")
    public class ProfileMenu {
        @Id
        private int id;

        @ManyToOne
        @JoinColumn(name = "profile_id")
        private Profile profile;

        private int userMenuId;

        private String status;
    }

    @Tuple
    @Data // lombok
    public class ProfileMenuGroup {

        private String profileMenuIds;

        private String description;
    }

    @Test
    // https://stackoverflow.com/questions/57393539/is-there-an-alternative-for-native-querys-group-concat-in-jpql-with-jpa
    public void GROUP_CONCAT1() {

        int profileId = 4;

        FluentQuery query = getMenuIdsByProfile(profileId);

        String expected = "SELECT GROUP_CONCAT(t1.user_menu_id SEPARATOR ',') AS profile_menu_ids, t0.description AS description "
                + "FROM profile t0  LEFT JOIN profile_menu t1  ON (t0.id = t1.profile_id) "
                + "WHERE ((t0.id = ?1) AND (t1.status = 'Y')) " + "GROUP BY  t0.id";

        assertQuery(query, expected);
    }

    private FluentQuery getMenuIdsByProfile(int profileId) {
        FluentQuery query = FluentJPA.SQL((Profile p,
                                           ProfileMenu pm) -> {
            String menuIds = alias(GROUP_CONCAT(pm.getUserMenuId(), ","), ProfileMenuGroup::getProfileMenuIds);
            String description = alias(p.getDescription(), ProfileMenuGroup::getDescription);

            SELECT(menuIds, description);
            FROM(p).LEFT_JOIN(pm).ON(p == pm.getProfile());
            WHERE(p.getId() == profileId && pm.getStatus() == "Y");
            GROUP(BY(p.getId()));
        });
        return query;// .createQuery(em, ProfileMenuGroup.class).getSingleResult();
    }

    @Test
    // https://stackoverflow.com/questions/57736227/conditional-join-fetch-in-hibernate
    public void TestMethodAccess() {

        FluentQuery query = FluentJPA.SQL((SecurityUserRealmRoleEntity roleRealm,
                                           SecurityRoleEntity role,
                                           JoinTable<SecurityRoleEntity, SecurityPermissionEntity> rolesToPermissions,
                                           SecurityPermissionEntity permission) -> {
            SELECT(roleRealm, permission.getId());
            FROM(roleRealm).JOIN(role)
                    .ON(roleRealm.getRole() == role)
                    .JOIN(rolesToPermissions)
                    .ON(rolesToPermissions.join(role, SecurityRoleEntity::getPermissions))
                    .JOIN(permission)
                    .ON(rolesToPermissions.inverseJoin(permission, SecurityRoleEntity::getPermissions));

            WHERE(role.isEnabled() && !role.isDeleted());
        });

        // @formatter:off
        String expected = "SELECT t0.*, t3.ID_PK "
                + "FROM BARBANETUSER.SECURITY_USER_REALM_ROLE t0  INNER JOIN BARBANETUSER.SECURITY_ROLE t1  ON (t0.ROLE_ID_FK = t1.ID_PK)  INNER JOIN BARBANETUSER.SECURITY_ROLE_PERMISSION t2  ON (t2.ROLE_ID_FK = t1.ID_PK)  INNER JOIN BARBANETUSER.SECURITY_PERMISSION t3  ON (t2.PERMISSION_ID_FK = t3.ID_PK) "
                + "WHERE (t1.ENABLED AND NOT(t1.DELETED))";
        // @formatter:on
        assertQuery(query, expected);
    }

    @Test
    // https://stackoverflow.com/questions/57701600/how-to-select-multiple-columns-in-subqueryhql
    public void TestSubQueryJoin() {

        FluentQuery query = FluentJPA.SQL((User u) -> {

            UserIdCount sub = subQuery((UserLog log) -> {
                int count = alias(COUNT(log.getUserId()), UserIdCount::getCount);
                SELECT(log.getUserId(), count);
                FROM(log);
                ORDER(BY(count).DESC());
                LIMIT(5);
            });

            SELECT(u.getName(), sub.getCount());
            FROM(u).JOIN(sub).ON(u.getId() == sub.getUserId());
        });

//        query.createQuery(em, UserNameCount.class).getSingleResult();

        // @formatter:off
        String expected = "SELECT t0.name, q0.count " + 
                "FROM USER t0  INNER JOIN (SELECT t1.user_id, COUNT(t1.user_id) AS count " + 
                "FROM USER_LOG t1 " + 
                "ORDER BY  COUNT(t1.user_id)  DESC   " + 
                "LIMIT 5 ) q0  ON (t0.id = q0.user_id)";
        // @formatter:on
        assertQuery(query, expected);
    }
}
