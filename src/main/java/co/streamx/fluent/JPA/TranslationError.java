package co.streamx.fluent.JPA;

import java.text.MessageFormat;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
enum TranslationError {
    INVALID_FROM_PARAM("Only @Entity, @Tuple or scalar(primitive) types are allowed in FROM context: {0}"),
    CANNOT_CALCULATE_TABLE_REFERENCE("Cannot calculate table reference from: {0}"),
    REQUIRES_EXTERNAL_PARAMETER(
            "Parameter method accepts external parameters only, as an object. "
                    + "Calculations and expressions must be performed out of Lambda. Received: {0}"),
    UNSUPPORTED_EXPRESSION_TYPE("Unsupported operator: {0}"),
    UNMAPPED_FIELD(
            "Cannot translate property: {0}. FluentJPA methods must be either static, interface default or annotated with @Local") {
        @Override
        public RuntimeException getError(Throwable cause,
                                         Object... args) {

            if (args[0].equals("equals")) {
                String format = MessageFormat.format(pattern, args) + ". Use == operator instead";
                return new UnsupportedOperationException(format, cause);
            }
            return super.getError(args);
        }
    },;
    protected final String pattern;

    public RuntimeException getError(Object... args) {
        return new IllegalStateException(MessageFormat.format(pattern, args));
    }

    public RuntimeException getError(Throwable cause,
                                     Object... args) {
        return new IllegalStateException(MessageFormat.format(pattern, args), cause);
    }
}
