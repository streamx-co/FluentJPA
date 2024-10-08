package co.streamx.fluent.SQL;

import static co.streamx.fluent.SQL.AggregateFunctions.COUNT;
import static co.streamx.fluent.SQL.AggregateFunctions.FIRST_VALUE;
import static co.streamx.fluent.SQL.AggregateFunctions.LAST_VALUE;
import static co.streamx.fluent.SQL.AggregateFunctions.NTH_VALUE;
import static co.streamx.fluent.SQL.AggregateFunctions.PERCENTILE_CONT;
import static co.streamx.fluent.SQL.Directives.aggregateBy;
import static co.streamx.fluent.SQL.Directives.alias;
import static co.streamx.fluent.SQL.Directives.viewOf;
import static co.streamx.fluent.SQL.Directives.windowFrame;
import static co.streamx.fluent.SQL.Library.COUNT;
import static co.streamx.fluent.SQL.PostgreSQL.SQL.GENERATE_SERIES;
import static co.streamx.fluent.SQL.PostgreSQL.SQL.registerVendorCapabilities;
import static co.streamx.fluent.SQL.SQL.BY;
import static co.streamx.fluent.SQL.SQL.FROM;
import static co.streamx.fluent.SQL.SQL.ORDER;
import static co.streamx.fluent.SQL.SQL.PARTITION;
import static co.streamx.fluent.SQL.SQL.SELECT;
import static co.streamx.fluent.SQL.SQL.WHERE;

import java.util.Date;

import jakarta.persistence.Table;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import co.streamx.fluent.JPA.CommonTest;
import co.streamx.fluent.JPA.FluentJPA;
import co.streamx.fluent.JPA.FluentQuery;
import co.streamx.fluent.notation.Tuple;
import lombok.Getter;

public class TestSQLAggregates implements CommonTest {

    @BeforeAll
    public static void init() {
        registerVendorCapabilities(FluentJPA::setCapabilities);
    }

    @Getter
    @Tuple
    @Table(name = "housholds")
    public static class Houshold {
        private int income;
        private double percentile;
    }

    @Test
    public void testWithinGroup() throws Exception {
        FluentQuery query = FluentJPA.SQL((Houshold h) -> {

            Double percentile = aggregateBy(PERCENTILE_CONT(0.5)).WITHIN_GROUP(ORDER(BY(h.getIncome())))
                    .AS(Houshold::getPercentile);
            SELECT(percentile);
            FROM(h);
        });

        String expected = "SELECT  PERCENTILE_CONT(0.5)  WITHIN GROUP(ORDER BY  t0.income  ) AS percentile "
                + "FROM housholds AS t0";

        assertQuery(query, expected);
    }

    @Getter
    @Tuple
    public static class Serie {
        private int iter;
    }

    @Test
    public void testFilter() throws Exception {
        FluentQuery query = FluentJPA.SQL(() -> {

            Serie series = GENERATE_SERIES(1, 10);

            Integer filtered = alias(aggregateBy(COUNT(series.getIter())).FILTER(WHERE(series.getIter() < 5)).AS() + 1,
                    "filtered");
            Integer unfiltered = alias(COUNT(), "unfiltered");

            SELECT(unfiltered, filtered);
            FROM(viewOf(series, Serie::getIter));
        });

        String expected = "SELECT COUNT(*) AS unfiltered, ( COUNT(t0.iter)  FILTER(WHERE (t0.iter < 5) ) + 1) AS filtered "
                + "FROM  GENERATE_SERIES(1, 10) AS t0 (iter)";

        assertQuery(query, expected);
    }

    @Getter
    @Tuple
    @Table(name = "observations")
    public static class Observation {
        private Date time;
        private String subject;
        private int val;
    }

    @Getter
    @Tuple
    public static class Stats {
        private int first;
        private int last;
        private int nth;
        private int nth4;
    }

    @Test
    public void testWindow1() throws Exception {
        FluentQuery query = FluentJPA.SQL((Observation o) -> {

            WindowDef w = PARTITION(BY(o.getSubject())).ORDER(BY(o.getTime()))
                    .ROWS(FrameBounds.UNBOUNDED_PRECEDING);

            WindowDef w1 = PARTITION(BY(o.getSubject())).ORDER(BY(o.getTime()))
                    .ROWS()
                    .BETWEEN(FrameBounds.UNBOUNDED_PRECEDING)
                    .AND(4, FrameBounds.FOLLOWING)
                    .EXCLUDE_CURRENT_ROW();

            WindowDef w2 = ORDER(BY(o.getTime()));

            WindowDef w3 = windowFrame().GROUPS(FrameBounds.CURRENT_ROW);

            Integer first = alias(aggregateBy(FIRST_VALUE(o.getVal())).OVER(w), Stats::getFirst);
            Integer last = alias(aggregateBy(LAST_VALUE(o.getVal())).OVER(w1), Stats::getLast);
            Integer nth = alias(aggregateBy(NTH_VALUE(o.getVal(), 2)).FILTER(WHERE(o.getVal() == 7)).OVER(w2),
                    Stats::getNth);
            Integer nth1 = alias(aggregateBy(NTH_VALUE(o.getVal(), 4)).OVER(w3), Stats::getNth4);

            SELECT(o.getTime(), o.getSubject(), o.getVal(), first, last, nth, nth1);
            FROM(o);
        });

        String expected = "SELECT t0.time, t0.subject, t0.val,  FIRST_VALUE(t0.val)  OVER(PARTITION BY  t0.subject   ORDER BY  t0.time   ROWS UNBOUNDED PRECEDING ) AS first,  LAST_VALUE(t0.val)  OVER(PARTITION BY  t0.subject   ORDER BY  t0.time   ROWS   BETWEEN UNBOUNDED PRECEDING  AND 4 FOLLOWING  EXCLUDE CURRENT ROW  ) AS last,  NTH_VALUE(t0.val, 2) FILTER (WHERE (t0.val = 7) ) OVER(ORDER BY  t0.time  ) AS nth,  NTH_VALUE(t0.val, 4)  OVER(   GROUPS CURRENT ROW ) AS nth4 "
                + "FROM observations AS t0";

        assertQuery(query, expected);
    }
}
