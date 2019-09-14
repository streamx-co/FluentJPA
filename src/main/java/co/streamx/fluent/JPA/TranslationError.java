package co.streamx.fluent.JPA;

import java.text.MessageFormat;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
enum TranslationError {
    INVALID_FROM_PARAM("Only @Entity, @Tuple or scalar(primitive) types are allowed in FROM context: {0}"),
    CANNOT_CALCULATE_TABLE_REFERENCE("Cannot calculate table reference from: {0}"),
    CANNOT_DEREFERENCE_PARAMETERS("Cannot access parameters properties inside SQL expression: [{0}].[{1}]"),
    REQUIRES_EXTERNAL_PARAMETER(
            "Parameter method accepts external parameters only, as an object. "
                    + "Calculations and expressions must be performed out of Lambda. Received: {0}"),
    UNSUPPORTED_EXPRESSION_TYPE("Unsupported operator: {0}"),
    INSTANCE_NOT_JOINTABLE("Not a JoinTable instance: {0}"),
    NOT_PROPERTY_CALL("Must pass a getter call: {0}"),
    ASSOCIATION_NOT_INITED("Association not initialized for {0}. Missed join() call?"),
    ALIAS_NOT_SPECIFIED("Alias not specified for index: {0}"),
    SECONDARY_TABLE_NOT_FOUND("Cannot find secondary table {1} declared on {0} entity"),
    SECONDARY_TABLE_NOT_CONSTANT("Secondary table name must be a constant. ({0})"),
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
