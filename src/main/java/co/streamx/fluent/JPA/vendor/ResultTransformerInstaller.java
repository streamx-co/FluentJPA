package co.streamx.fluent.JPA.vendor;

import jakarta.persistence.Query;

public interface ResultTransformerInstaller {
    void install(Query query,
                 TupleResultTransformer<?> transformer);
}
