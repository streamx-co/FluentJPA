package co.streamx.fluent.JPA;

import static co.streamx.fluent.SQL.AggregateFunctions.COUNT;
import static co.streamx.fluent.SQL.AggregateFunctions.MAX;
import static co.streamx.fluent.SQL.AggregateFunctions.ROW_NUMBER;
import static co.streamx.fluent.SQL.AggregateFunctions.SUM;
import static co.streamx.fluent.SQL.Directives.aggregateBy;
import static co.streamx.fluent.SQL.Directives.alias;
import static co.streamx.fluent.SQL.Directives.discardSQL;
import static co.streamx.fluent.SQL.Directives.injectSQL;
import static co.streamx.fluent.SQL.Directives.parameter;
import static co.streamx.fluent.SQL.Directives.recurseOn;
import static co.streamx.fluent.SQL.Directives.subQuery;
import static co.streamx.fluent.SQL.Library.pick;
import static co.streamx.fluent.SQL.Library.selectAll;
import static co.streamx.fluent.SQL.MySQL.SQL.GROUP_CONCAT;
import static co.streamx.fluent.SQL.MySQL.SQL.IF;
import static co.streamx.fluent.SQL.MySQL.SQL.LIMIT;
import static co.streamx.fluent.SQL.MySQL.SQL.STR_TO_DATE;
import static co.streamx.fluent.SQL.Operators.BETWEEN;
import static co.streamx.fluent.SQL.Operators.UNION_ALL;
import static co.streamx.fluent.SQL.Operators.lessEqual;
import static co.streamx.fluent.SQL.Oracle.SQL.TO_DATE;
import static co.streamx.fluent.SQL.Oracle.SQL.registerVendorCapabilities;
import static co.streamx.fluent.SQL.SQL.BY;
import static co.streamx.fluent.SQL.SQL.DISTINCT;
import static co.streamx.fluent.SQL.SQL.FROM;
import static co.streamx.fluent.SQL.SQL.GROUP;
import static co.streamx.fluent.SQL.SQL.HAVING;
import static co.streamx.fluent.SQL.SQL.INSERT;
import static co.streamx.fluent.SQL.SQL.ORDER;
import static co.streamx.fluent.SQL.SQL.PARTITION;
import static co.streamx.fluent.SQL.SQL.RECURSIVE;
import static co.streamx.fluent.SQL.SQL.SELECT;
import static co.streamx.fluent.SQL.SQL.UPDATE;
import static co.streamx.fluent.SQL.SQL.VALUES;
import static co.streamx.fluent.SQL.SQL.WHERE;
import static co.streamx.fluent.SQL.SQL.WITH;
import static co.streamx.fluent.SQL.SQL.row;
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

import co.streamx.fluent.SQL.Alias;
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

    @Test
    // https://stackoverflow.com/questions/57803756/hibernate-many-to-many-contains-specific-ids-using-criteria
    public void TestMTMIds() {

        List<String> categoryIds = Arrays.asList("a", "b");
        getMoviesByCategories(categoryIds);
    }

    public void getMoviesByCategories(List<String> categoryIds) {
        int matchTotal = categoryIds.size();

        FluentQuery query = FluentJPA.SQL((Movie movie,
                                           JoinTable<Movie, Category> movieCategory) -> {

            discardSQL(movieCategory.join(movie, Movie::getCategories));

            List<String> movieIds = subQuery(() -> {
                String movieId = movieCategory.getJoined().getId();
                String catId = movieCategory.getInverseJoined().getId();

                SELECT(movieId);
                FROM(movieCategory);
                WHERE(categoryIds.contains(catId));
                GROUP(BY(movieId));
                HAVING(COUNT(movieId) == matchTotal); // COUNT(DISTINCT(movieId));
            });

            SELECT(movie);
            FROM(movie);
            WHERE(movieIds.contains(movie.getId()));

        });

//        query.createQuery(em, Movie.class).getResultList();

        // @formatter:off
        String expected = "SELECT t0.* " + 
                "FROM t0 " + 
                "WHERE (t0.id IN (SELECT t1.movie_id " + 
                "FROM movie_category t1 " + 
                "WHERE (t1.CATEGORIES_id IN ?1 ) " + 
                "GROUP BY  t1.movie_id  " + 
                "HAVING (COUNT(t1.movie_id) = ?2) ) )";
        // @formatter:on
        assertQuery(query, expected, arrayOf(categoryIds, matchTotal));
    }

    @Tuple
    @Data
    public static class StudentMarks {
        private String name;
        private int math;
        private int physics;
        private int chemistry;
    }

    @Test
    // https://stackoverflow.com/questions/57800309/jpa-criteria-api-aggregating-on-multiple-columns-with-if-condition
    public void testMaxIf() {

        FluentQuery query = FluentJPA.SQL((Student st,
                                           Subject sub) -> {
            Integer math = alias(MAX(IF(sub.getTitle() == "math", sub.getMarks(), 0)), StudentMarks::getMath);
            Integer physics = alias(MAX(IF(sub.getTitle() == "physics", sub.getMarks(), 0)), StudentMarks::getPhysics);
            Integer chemistry = alias(MAX(IF(sub.getTitle() == "chemistry", sub.getMarks(), 0)),
                    StudentMarks::getChemistry);

            SELECT(st.getName(), math, physics, chemistry);
            FROM(st).LEFT_JOIN(sub).ON(sub.getStudent() == st);
            GROUP(BY(st.getName()));
        });

//        query.createQuery(em, UserNameCount.class).getSingleResult();

        // @formatter:off
        String expected = "SELECT t0.name, MAX(IF((t1.title = 'math'), t1.marks, 0)) AS math, MAX(IF((t1.title = 'physics'), t1.marks, 0)) AS physics, MAX(IF((t1.title = 'chemistry'), t1.marks, 0)) AS chemistry " + 
                "FROM STUDENTS t0  LEFT JOIN SUBJECTS t1  ON (t1.student_id = t0.id) " + 
                "GROUP BY  t0.name";
        // @formatter:on
        assertQuery(query, expected);
    }

    @Tuple
    @Getter
    public static class OrderedErrorRecord extends ErrorRecord {
        private int rowNumber;
    }

    @Tuple
    @Getter
    public static class ErrorTypeWithLastError extends ErrorType {
        @Embedded
        private ErrorContent errorContent;
    }

    @Test
    // https://stackoverflow.com/questions/57780699/javahibernate-how-to-fetch-limit-of-child-dependency-collection
    public void testLastChild() {

        int deviceId = 8;

        FluentQuery query = FluentJPA.SQL((ErrorType errorType) -> {

            OrderedErrorRecord orderedRec = subQuery((ErrorRecord errorRec) -> {
                Alias<Number> rn = alias(aggregateBy(ROW_NUMBER())
                        .OVER(PARTITION(BY(errorRec.getErrorType().getId())).ORDER(BY(errorRec.getCreatedAt()).DESC())),
                        OrderedErrorRecord::getRowNumber);

                SELECT(errorRec, rn);
                FROM(errorRec);
            });

            WITH(orderedRec);
            SELECT(errorType, orderedRec.getErrorDescription());
            FROM(errorType).LEFT_JOIN(orderedRec)
                    .ON(orderedRec.getErrorType() == errorType && orderedRec.getRowNumber() == 1);
            WHERE(errorType.getDevice().getId() == deviceId);
        });

//        query.createQuery(em, UserNameCount.class).getSingleResult();

        // @formatter:off
        String expected = "WITH q0 AS " + 
                "(SELECT t1.*,  ROW_NUMBER()  OVER(PARTITION BY  t1.error_type_id   ORDER BY  t1.created_at  DESC   ) AS row_number " + 
                "FROM tbl_error_record t1 ) " + 
                "SELECT t0.*, q0.error_description " + 
                "FROM tbl_error_type t0  LEFT JOIN q0  ON ((q0.error_type_id = t0.id) AND (q0.row_number = 1)) " +
                "WHERE (t0.device_id = ?1)";
        // @formatter:on
        assertQuery(query, expected, arrayOf(deviceId));
    }

    @Test
    // https://stackoverflow.com/questions/57831475/search-with-the-supplied-value-in-the-current-and-all-successive-parents-when-bo
    public void testSuccParents() {

        String aggKey = "aggKey";
        String aggValue = "aggValue";

        FluentQuery query = getDetailsByAggregator(aggKey, aggValue);

        // @formatter:off
        String expected = "WITH RECURSIVE q0  AS " + 
                "(SELECT t2.* " + 
                "FROM QUALITY_AGGREGATOR t2 " + 
                "WHERE ((t2.agg_key = ?1) AND (t2.agg_value = ?2)) " + 
                "UNION ALL  " + 
                "SELECT t3.* " + 
                "FROM QUALITY_AGGREGATOR t3  INNER JOIN q0 t1  ON (t3.parent_agg_id = t1.agg_id) )" + 
                
                "SELECT DISTINCT t0.* " + 
                "FROM INFLUENCE_DETAILS t0  INNER JOIN q0  ON (t0.aggregator_id = q0.agg_id)";
        // @formatter:on
        assertQuery(query, expected, arrayOf(aggKey, aggValue));
    }

    private FluentQuery getDetailsByAggregator(String aggKey,
                                               String aggValue) {
        FluentQuery query = FluentJPA.SQL((InfluenceDetails details) -> {

            QualityAggregator relevantAgg = subQuery((QualityAggregator it,
                                                      QualityAggregator agg,
                                                      QualityAggregator child) -> {
                SELECT(agg);
                FROM(agg);
                WHERE(agg.getAggKey() == aggKey && agg.getAggValue() == aggValue);

                UNION_ALL();

                SELECT(child);
                FROM(child).JOIN(recurseOn(it)).ON(child.getParent() == it);
            });

            WITH(RECURSIVE(relevantAgg));

            SELECT(DISTINCT(details));
            FROM(details).JOIN(relevantAgg).ON(details.getAggregators() == relevantAgg);
        });

//      query.createQuery(em, UserNameCount.class).getSingleResult();

        return query;
    }

    @Test
    // https://stackoverflow.com/questions/57887521/hibernate-set-an-manytoone-column-value-just-with-id-without-an-instance-of-o
    public void testInsertMTMId() {

        int productId = 5;

        FluentQuery query = createOrderProduct(productId);

        // @formatter:off
        String expected = "INSERT   INTO order_product t0 " + 
                "VALUES (?1)";
        // @formatter:on
        assertQuery(query, expected, arrayOf(productId));
    }

    private FluentQuery createOrderProduct(int productId) {
        FluentQuery query = FluentJPA.SQL((OrderProduct op) -> {
            INSERT().INTO(op);
            VALUES(row(productId));
        });
        return query;
    }

    @Tuple
    @Data
    public static class MultiTab {
        long id;
        long projectId;
        long empId;
        @Column(name = "projName")
        String projectName;
        @Column(name = "empName")
        String empName;
    }

    @Test
    // https://stackoverflow.com/questions/57907574/jpa-criteria-query-select-from-3-tables-based-on-id
    public void testJoinNoCondition() {

        FluentQuery query = FluentJPA.SQL((Table1 t1,
                                           Table2 t2,
                                           Table3 t3) -> {
            SELECT(t1.getId(), t1.getProjectId(), t1.getEmpId(), alias(t2.getProjectName(), MultiTab::getProjectName),
                    t3.getEmpName());
            FROM(t1, t2, t3);
            WHERE(t1.getEmpId() == t3.getId() && t1.getProjectId() == t2.getId());
        });

        // @formatter:off
        String expected = "SELECT t0.id, t0.project_id, t0.emp_id, t1.projectName AS projName, t2.empName " + 
                "FROM tab1 t0, tab2 t1, tab3 t2 " + 
                "WHERE ((t0.emp_id = t2.id) AND (t0.project_id = t1.id))";
        // @formatter:on
        assertQuery(query, expected);
    }

    @Test
    // https://stackoverflow.com/questions/57925877/spring-data-jpa-class-based-projections-with-custom-query
    public void testClassBasedProjections() {

        FluentQuery query = findDistinctQuery();

        // @formatter:off
        String expected = "SELECT DISTINCT t0.first_name, t0.last_name  " + 
                "FROM t0";
        // @formatter:on
        assertQuery(query, expected);
    }

    public FluentQuery findDistinctQuery() {
        FluentQuery query = FluentJPA.SQL((Person p) -> {
            SELECT(DISTINCT(p.getFirstName(), p.getLastName()));
            FROM(p);
        });
        return query;
    }

    @Test
    // https://stackoverflow.com/questions/57925877/spring-data-jpa-class-based-projections-with-custom-query
    public void testFindMerchants() {

        Integer[] criteria = { 1, 2, 3 };
        List<Integer[]> params = Arrays.asList(criteria, criteria);

        FluentQuery query = findMerchants(params);

        // @formatter:off
        String expected = "SELECT t0.* " + 
                "FROM t0 " + 
                "WHERE (((t0.brand = 1) AND ((t0.merchant = 2) AND (t0.category = 3))) OR ((t0.brand = 1) AND ((t0.merchant = 2) AND (t0.category = 3))))";
        // @formatter:on
        assertQuery(query, expected);
    }

    public FluentQuery findMerchants(List<Integer[]> params) {

        Function1<BrandMerchant, Boolean> dynamicFilter = buildOr(params);

        FluentQuery query = FluentJPA.SQL((BrandMerchant m) -> {
            SELECT(m);
            FROM(m);
            WHERE(dynamicFilter.apply(m));
        });
        return query;
    }

    private Function1<BrandMerchant, Boolean> buildOr(List<Integer[]> params) {
        Function1<BrandMerchant, Boolean> criteria = Function1.FALSE();

        for (Integer[] tuple : params) {
            int brandId = tuple[0];
            int merchantId = tuple[1];
            int categoryId = tuple[2];
            criteria = criteria.or(m -> m.getBrand().getId() == brandId && m.getMerchant().getId() == merchantId
                    && m.getCategory().getId() == categoryId);
        }

        return criteria;
    }
}
