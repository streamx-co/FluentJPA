package co.streamx.fluent.JPA;

import static co.streamx.fluent.SQL.AggregateFunctions.MIN;
import static co.streamx.fluent.SQL.AggregateFunctions.ROW_NUMBER;
import static co.streamx.fluent.SQL.Directives.aggregateBy;
import static co.streamx.fluent.SQL.Directives.subQuery;
import static co.streamx.fluent.SQL.Library.pick;
import static co.streamx.fluent.SQL.Library.selectAll;
import static co.streamx.fluent.SQL.Library.selectMany;
import static co.streamx.fluent.SQL.Operators.add;
import static co.streamx.fluent.SQL.Operators.less;
import static co.streamx.fluent.SQL.Operators.subtract;
import static co.streamx.fluent.SQL.PostgreSQL.SQL.DATE_TRUNC;
import static co.streamx.fluent.SQL.PostgreSQL.SQL.EXTRACT;
import static co.streamx.fluent.SQL.PostgreSQL.SQL.NOW;
import static co.streamx.fluent.SQL.PostgreSQL.SQL.POSITION;
import static co.streamx.fluent.SQL.PostgreSQL.SQL.SUBSTR;
import static co.streamx.fluent.SQL.SQL.BY;
import static co.streamx.fluent.SQL.SQL.FROM;
import static co.streamx.fluent.SQL.SQL.ORDER;
import static co.streamx.fluent.SQL.SQL.PARTITION;
import static co.streamx.fluent.SQL.SQL.SELECT;
import static co.streamx.fluent.SQL.SQL.WHERE;
import static co.streamx.fluent.SQL.ScalarFunctions.BOTH;
import static co.streamx.fluent.SQL.ScalarFunctions.LEFT;
import static co.streamx.fluent.SQL.ScalarFunctions.RIGHT;
import static co.streamx.fluent.SQL.ScalarFunctions.TRIM;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import co.streamx.fluent.SQL.PostgreSQL.DataTypes;
import co.streamx.fluent.SQL.PostgreSQL.DatePart;

public class ModeComTutorial implements CommonTest, ModeComTutorialTypes {

    @BeforeAll
    public static void init() {
        FluentJPA.setCapabilities(Collections.emptySet());
    }

    @Test
    public void testDateTime() throws Exception {
        FluentQuery query = FluentJPA.SQL((CrunchbaseCompany c,
                                           CrunchbaseAcquisition a) -> {

            Timestamp timeToAck = subtract(a.getAcquiredAtCleaned(), DataTypes.TIMESTAMP.cast(c.getFoundedAtClean()));
            SELECT(c.getPermalink(), c.getFoundedAtClean(), a.getAcquiredAtCleaned(), timeToAck);
            FROM(c).JOIN(a).ON(a.getCompanyPermalink() == c.getPermalink());
            WHERE(c.getFoundedAtClean() != null);

        });

        String expected = "SELECT t0.permalink, t0.founded_at_clean, t1.acquired_at_cleaned, (t1.acquired_at_cleaned - CAST(t0.founded_at_clean AS TIMESTAMP) ) "
                + "FROM tutorial.crunchbase_companies_clean_date t0  INNER JOIN tutorial.crunchbase_acquisitions_clean_date t1  ON (t1.company_permalink = t0.permalink) "
                + "WHERE (t0.founded_at_clean IS NOT NULL)";

        assertQuery(query, expected);
    }

    @Test
    public void testDateTime1() throws Exception {
        FluentQuery query = FluentJPA.SQL((CrunchbaseCompany c) -> {

            Timestamp plusOneWeek = add(DataTypes.TIMESTAMP.cast(c.getFoundedAtClean()),
                    DataTypes.INTERVAL.literal("1 week"));
            SELECT(c.getPermalink(), c.getFoundedAtClean(), plusOneWeek);
            FROM(c);
            WHERE(c.getFoundedAtClean() != null);

        });

        String expected = "SELECT t0.permalink, t0.founded_at_clean, (CAST(t0.founded_at_clean AS TIMESTAMP) + INTERVAL  '1 week'  ) "
                + "FROM tutorial.crunchbase_companies_clean_date t0 " + "WHERE (t0.founded_at_clean IS NOT NULL)";

        assertQuery(query, expected);
    }

    @Test
    public void testDateTime2() throws Exception {
        FluentQuery query = FluentJPA.SQL((CrunchbaseCompany c) -> {

            Date foundedAgo = subtract(NOW(), DataTypes.TIMESTAMP.cast(c.getFoundedAtClean()));
            SELECT(c.getPermalink(), c.getFoundedAtClean(), foundedAgo);
            FROM(c);
            WHERE(c.getFoundedAtClean() != null);

        });

        String expected = "SELECT t0.permalink, t0.founded_at_clean, (NOW() - CAST(t0.founded_at_clean AS TIMESTAMP) ) "
                + "FROM tutorial.crunchbase_companies_clean_date t0 " + "WHERE (t0.founded_at_clean IS NOT NULL)";

        assertQuery(query, expected);
    }

    @Test
    public void testString() throws Exception {
        FluentQuery query = FluentJPA.SQL((CrunchbaseCompany c) -> {

            SELECT(c.getPermalink(), c.getFoundedAtClean(), LEFT(c.getFoundedAtClean(), 10),
                    RIGHT(c.getFoundedAtClean(), 10));
            FROM(c);
            WHERE(c.getFoundedAtClean() != null);

        });

        String expected = "SELECT t0.permalink, t0.founded_at_clean, LEFT(t0.founded_at_clean, 10), RIGHT(t0.founded_at_clean, 10) "
                + "FROM tutorial.crunchbase_companies_clean_date t0 " + "WHERE (t0.founded_at_clean IS NOT NULL)";

        assertQuery(query, expected);
    }

    @Test
    public void testStringTrim() throws Exception {
        FluentQuery query = FluentJPA.SQL((CrimeIncidents2014_01 ci) -> {

            SELECT(ci.getLocation(), TRIM(BOTH("()").FROM(ci.getLocation())), POSITION("A", ci.getDescript()));
            FROM(ci);

        });

        String expected = "SELECT t0.location, TRIM(BOTH '()' FROM t0.location), POSITION('A' IN t0.descript) "
                + "FROM tutorial.sf_crime_incidents_2014_01 t0";

        assertQuery(query, expected);
    }

    @Test
    public void testStringToDate() throws Exception {
        FluentQuery query = FluentJPA.SQL((CrimeIncidents2014_01 ci) -> {

            String cleaned = SUBSTR(ci.getDate(), 7, 4) + "-" + LEFT(ci.getDate(), 2) + "-"
                    + SUBSTR(ci.getDate(), 4, 2);

            SELECT(ci.getIncidntNum(), ci.getDate(), DataTypes.DATE.cast(cleaned));
            FROM(ci);

        });

        String expected = "SELECT t0.incidnt_num, t0.date, CAST( CONCAT( CONCAT( CONCAT( CONCAT(  SUBSTR(t0.date, 7, 4)  ,  '-' ) ,  LEFT(t0.date, 2) ) ,  '-' ) ,  SUBSTR(t0.date, 4, 2) )  AS DATE) "
                + "FROM tutorial.sf_crime_incidents_2014_01 t0";

        try {
            assertQuery(query, expected);
        } catch (AssertionError e) {
            expected = "SELECT t0.incidnt_num, t0.date, CAST( CONCAT( CONCAT( CONCAT( CONCAT( CONCAT( '' , SUBSTR(t0.date, 7, 4) ) , '-' ) , LEFT(t0.date, 2) ) , '-' ) , SUBSTR(t0.date, 4, 2) ) AS DATE) FROM tutorial.sf_crime_incidents_2014_01 t0";
            assertQuery(query, expected);
        }
    }

    @Test
    public void testDateExtract() throws Exception {
        FluentQuery query = FluentJPA.SQL((CrunchbaseAcquisition a) -> {

            SELECT(EXTRACT(DatePart.YEAR, a.getAcquiredAtCleaned()),
                    DATE_TRUNC(DatePart.QUARTER, a.getAcquiredAtCleaned()));
            FROM(a);

        });

        String expected = "SELECT EXTRACT(YEAR FROM t0.acquired_at_cleaned), DATE_TRUNC('QUARTER', t0.acquired_at_cleaned) "
                + "FROM tutorial.crunchbase_acquisitions_clean_date t0";

        assertQuery(query, expected);
    }

    @Test
    public void testSubQueryCond() throws Exception {
        FluentQuery query = FluentJPA.SQL((CrimeIncidents2014_01 ci) -> {

            selectAll(ci);
            WHERE(ci.getDate() == pick(ci, MIN(ci.getDate())));

        });

        String expected = "SELECT t0.* " + "FROM tutorial.sf_crime_incidents_2014_01 t0 "
                + "WHERE (t0.date = (SELECT MIN(t0.date) " + "FROM tutorial.sf_crime_incidents_2014_01 t0 ))";

        assertQuery(query, expected);
    }

    @Test
    public void testSubQueryBasic() throws Exception {
        FluentQuery query = FluentJPA.SQL((CrimeIncidents2014_01 ci1) -> {

            CrimeIncidents2014_01 friCI = subQuery((CrimeIncidents2014_01 ci) -> {

                selectAll(ci);
                WHERE(ci.getDayOfWeek() == "Friday");

            });

            selectAll(friCI);
            WHERE(friCI.getResolution() == "NONE");

        });

        String expected = "SELECT q0.* " + "FROM (SELECT t1.* " + "FROM tutorial.sf_crime_incidents_2014_01 t1 "
                + "WHERE (t1.day_of_week = 'Friday') ) q0 "
                + "WHERE (q0.resolution = 'NONE')";

        assertQuery(query, expected);
    }

    @Test
    public void testWin() throws Exception {
        FluentQuery query = FluentJPA.SQL((BikeShareQ12012 bs) -> {

            Long rowNumber = aggregateBy(ROW_NUMBER()).OVER(ORDER(BY(bs.getStartTime()))).AS("row_number");

            selectMany(bs, bs.getStartTerminal(), bs.getStartTime(), bs.getDurationSeconds(), rowNumber);
            WHERE(less(bs.getStartTime(), DataTypes.TIMESTAMP.literal("2012-01-08")));

        });

        String expected = "SELECT t0.start_terminal, t0.start_time, t0.duration_seconds,  ROW_NUMBER()  OVER(ORDER BY  t0.start_time  ) AS row_number "
                + "FROM tutorial.dc_bikeshare_q1_2012 t0 " + "WHERE (t0.start_time < TIMESTAMP  '2012-01-08'  )";

        assertQuery(query, expected);
    }

    @Test
    public void testWin1() throws Exception {
        FluentQuery query = FluentJPA.SQL((BikeShareQ12012 bs) -> {

            Long rowNumber = aggregateBy(ROW_NUMBER())
                    .OVER(PARTITION(BY(bs.getStartTerminal())).ORDER(BY(bs.getStartTime())))
                    .AS("row_number");

            selectMany(bs, bs.getStartTerminal(), bs.getStartTime(), bs.getDurationSeconds(), rowNumber);
            WHERE(less(bs.getStartTime(), DataTypes.TIMESTAMP.literal("2012-01-08")));

        });

        String expected = "SELECT t0.start_terminal, t0.start_time, t0.duration_seconds,  ROW_NUMBER()  OVER(PARTITION BY  t0.start_terminal   ORDER BY  t0.start_time  ) AS row_number "
                + "FROM tutorial.dc_bikeshare_q1_2012 t0 " + "WHERE (t0.start_time < TIMESTAMP  '2012-01-08'  )";

        assertQuery(query, expected);
    }
}
