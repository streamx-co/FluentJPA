package co.streamx.fluent.JPA.repository;

import static co.streamx.fluent.SQL.AggregateFunctions.LAG;
import static co.streamx.fluent.SQL.AggregateFunctions.MAX;
import static co.streamx.fluent.SQL.AggregateFunctions.MIN;
import static co.streamx.fluent.SQL.Directives.aggregateBy;
import static co.streamx.fluent.SQL.Directives.alias;
import static co.streamx.fluent.SQL.Directives.subQuery;
import static co.streamx.fluent.SQL.Library.collect;
import static co.streamx.fluent.SQL.Operators.lessEqual;
import static co.streamx.fluent.SQL.SQL.BY;
import static co.streamx.fluent.SQL.SQL.FROM;
import static co.streamx.fluent.SQL.SQL.GROUP;
import static co.streamx.fluent.SQL.SQL.HAVING;
import static co.streamx.fluent.SQL.SQL.PARTITION;
import static co.streamx.fluent.SQL.SQL.SELECT;
import static co.streamx.fluent.SQL.SQL.WHERE;
import static co.streamx.fluent.SQL.ScalarFunctions.CASE;
import static co.streamx.fluent.SQL.ScalarFunctions.WHEN;

import java.math.BigInteger;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import co.streamx.fluent.JPA.FluentJPA;
import co.streamx.fluent.JPA.FluentQuery;
import co.streamx.fluent.JPA.repository.entities.NetworkObject;
import co.streamx.fluent.JPA.repository.entities.NetworkObjectRange;
import co.streamx.fluent.JPA.repository.entities.ObjectContainer;
import co.streamx.fluent.notation.Tuple;
import lombok.Getter;

public interface NetworkObjectRepo extends JpaRepository<NetworkObject, Long>, EntityManagerSupplier {
    default List<NetworkObject> getAllNative() {
        FluentQuery query = FluentJPA.SQL((NetworkObject net) -> {
            SELECT(net);
            FROM(net);
        });

        return query.createQuery(getEntityManager(), NetworkObject.class).getResultList();
    }

    @Tuple
    @Getter
    class NetworkObjectRangeWithParam extends NetworkObjectRange {
        private long param;
    }

    static Long aggregateWindow(NetworkObjectRange netRange,
                       long agg) {
        return aggregateBy(agg)
                .OVER(PARTITION(BY(netRange.getNetworkObject()))
                        .ORDER(BY(netRange.getFirst()), BY(netRange.getLast())))
                .AS();
    }

    default List<NetworkObject> findInRange(List<String> objectContainerNames,
                                            BigInteger maxIpCount,
                                            List<Integer> objectInternalTypes,
                                            long minRange,
                                            long maxRange) {

        FluentQuery query = FluentJPA.SQL((NetworkObject net,
                                           ObjectContainer objcon,
                                           NetworkObjectRange netRange) -> {

            NetworkObjectRangeWithParam s1 = subQuery(() -> {
                long le = aggregateWindow(netRange, LAG(netRange.getLast())) + 1;

                SELECT(netRange.getNetworkObject().getId(), netRange.getFirst(), netRange.getLast(),
                        alias(le, NetworkObjectRangeWithParam::getParam));
                FROM(netRange);
            });

            NetworkObjectRangeWithParam s2 = subQuery(() -> {

                long maxLE = aggregateWindow(s1, MAX(s1.getParam()));
                Long new_start = CASE(WHEN(s1.getFirst() <= maxLE).THEN((Long) null).ELSE(s1.getFirst())).END();

                SELECT(s1.getNetworkObject().getId(), s1.getFirst(), s1.getLast(),
                        alias(new_start, NetworkObjectRangeWithParam::getParam));
                FROM(s1);
            });

            NetworkObjectRangeWithParam s3 = subQuery(() -> {

                long left_edge = aggregateWindow(s2, MAX(s2.getParam()));

                SELECT(s2.getNetworkObject().getId(), s2.getFirst(), s2.getLast(),
                        alias(left_edge, NetworkObjectRangeWithParam::getParam));
                FROM(s2);
            });

            NetworkObjectRange netObjectRange = subQuery(() -> {
                SELECT(s3);
                FROM(s3);
                GROUP(BY(s3.getNetworkObject()), BY(s3.getParam()));
                HAVING(MIN(s3.getFirst()) <= minRange && MAX(s3.getLast()) >= maxRange);
            });

            SELECT(net);
            FROM(net).JOIN(objcon).ON(net.getObjectContainer() == objcon);
            WHERE(objectContainerNames.contains(objcon.getName()) && lessEqual(net.getIpCount(), maxIpCount)
                    && objectInternalTypes.contains(net.getObjectInternalType())
                    && collect(netObjectRange, netObjectRange.getNetworkObject().getId()).contains(net.getId()));
        });

        return query.createQuery(getEntityManager(), NetworkObject.class).getResultList();
    }
}
