package co.streamx.fluent.JPA;

public interface UnboundCharSequence extends CharSequence {
    default boolean isEmpty() {
        return length() == 0;
    }
}
