package io.soabase.structured.logger;

import io.soabase.structured.logger.generator.Generator;
import org.slf4j.Logger;

import java.util.Map;
import java.util.function.Consumer;

public class StructuredLoggerFactory {
    private static final Generator generator = new Generator();
    private final LoggingFormatter loggingFormatter;

    public <T> StructuredLogger<T> getLogger(Logger logger, Class<T> schema) {
        return get(logger, schema, loggingFormatter);
    }

    public static StructuredLoggerFactory withLoggingFormatter(LoggingFormatter loggingFormatter) {
        return new StructuredLoggerFactory(loggingFormatter);
    }

    public static <T> StructuredLogger<T> get(Logger logger, Class<T> schema) {
        return get(logger, schema, LoggingFormatter.defaultLoggingFormatter);
    }

    public static <T> StructuredLogger<T> get(Logger logger, Class<T> schema, LoggingFormatter loggingFormatter) {
        T schemaInstance = newSchemaInstance(logger, schema);
        return new StructuredLogger<T>() {
            @Override
            public Logger logger() {
                return logger;
            }

            @Override
            public void trace(Consumer<T> statement, String mainMessage) {
                if (logger.isTraceEnabled()) {
                    consume(statement, mainMessage, logger::trace);
                }
            }

            @Override
            public void debug(Consumer<T> statement, String mainMessage) {
                if (logger.isDebugEnabled()) {
                    consume(statement, mainMessage, logger::debug);
                }
            }

            @Override
            public void warn(Consumer<T> statement, String mainMessage) {
                if (logger.isWarnEnabled()) {
                    consume(statement, mainMessage, logger::warn);
                }
            }

            @Override
            public void info(Consumer<T> statement, String mainMessage) {
                if (logger.isInfoEnabled()) {
                    consume(statement, mainMessage, logger::info);
                }
            }

            @Override
            public void error(Consumer<T> statement, String mainMessage) {
                if (logger.isErrorEnabled()) {
                    consume(statement, mainMessage, logger::error);
                }
            }

            private void consume(Consumer<T> statement, String mainMessage, Consumer<String> logger)
            {
                statement.accept(schemaInstance);
                Map<String, Object> values = ((Instance) schemaInstance).slogGetValues();
                logger.accept(loggingFormatter.format(mainMessage, values));
            }
        };
    }

    private static <T> T newSchemaInstance(Logger logger, Class<T> schema) {
        Class<T> generate = generator.generate(schema, logger.getClass().getClassLoader());
        T schemaInstance = null;
        try {
            schemaInstance = generate.newInstance();
        } catch (Exception e) {
            // TODO
        }
        return schemaInstance;
    }

    private StructuredLoggerFactory(LoggingFormatter loggingFormatter) {
        this.loggingFormatter = loggingFormatter;
    }
}
