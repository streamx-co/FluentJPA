package co.streamx.fluent.JPA;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public interface CommonTest {

    default void assertQuery(FluentQuery query,
                             String expected) {
        String sql = query.toString();
        System.out.println(sql);
        System.out.println(query.getOrdinalParameters());

        assertEquals(normalizeSpaces(expected), normalizeSpaces(sql));
    }

    static String normalizeSpaces(String string) {
        return string.replaceAll("\\s*\\(\\s*", " (")
                .replaceAll("\\s*\\)\\s*", ") ")
                .replaceAll("\\s*\\,\\s*", ",")
                .replaceAll("\\s+", " ")
                .replace("\n", "");
    }

    default void assertQuery(FluentQuery query,
                             String expected,
                             Object[] expectedArguments) {
        assertQuery(query, expected);
        assertArrayEquals(expectedArguments, query.getOrdinalParameters().toArray());
    }

    default Object[] arrayOf(Object... objects) {
        return objects;
    }
}
