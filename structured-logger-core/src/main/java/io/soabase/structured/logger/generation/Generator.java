/**
 * Copyright 2019 Jordan Zimmerman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.soabase.structured.logger.generation;

import io.soabase.structured.logger.exception.InvalidSchemaException;
import io.soabase.structured.logger.formatting.LoggingFormatter;
import io.soabase.structured.logger.schemas.WithFormat;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.Implementation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.bytebuddy.implementation.MethodCall.invoke;
import static net.bytebuddy.matcher.ElementMatchers.named;

public class Generator {
    private final Map<Key, Generated> generated = new ConcurrentHashMap<>();
    private static final Set<String> reservedMethodNames = Collections.unmodifiableSet(
            Stream.of(Instance.class.getMethods()).map(Method::getName).collect(Collectors.toSet())
    );
    private static final Method setValueAtIndexMethod;
    private static final Method formattedAtIndexMethod;
    static {
        try {
            setValueAtIndexMethod = Instance.class.getMethod("_InternalSetValueAtIndex", Integer.TYPE, Object.class);
            formattedAtIndexMethod = Instance.class.getMethod("_InternalFormattedAtIndex", Integer.TYPE, String.class, Object[].class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Could not find internal method", e);
        }
    }

    public void clearCache() {
        generated.clear();
    }

    @SuppressWarnings("unchecked")
    public <T> Generated<T> generate(Class<T> schemaClass, ClassLoader classLoader, LoggingFormatter loggingFormatter) {
        return generated.computeIfAbsent(new Key(schemaClass, loggingFormatter), __ -> {
            List<String> schemaNames = validateSchemaClass(schemaClass);
            Class generatedClass = internalGenerate(schemaClass, classLoader, loggingFormatter);
            return new GeneratedImpl<>(generatedClass, schemaNames, loggingFormatter);
        });
    }

    private <T> List<String> validateSchemaClass(Class<T> schemaClass) {
        if (!schemaClass.isInterface()) {
            throw new InvalidSchemaException("Schema must be an interface. Schema: " + schemaClass.getName());
        }

        List<String> schemaNames = new ArrayList<>();
        Set<String> usedMethodNames = new HashSet<>();
        for (Method method : schemaClass.getMethods()) {
            if (!method.getReturnType().isAssignableFrom(schemaClass)) {
                throw new InvalidSchemaException("Schema methods must return " + schemaClass.getSimpleName() + " or a subclass of it. Method: " + method.getName());
            }
            if (!usedMethodNames.add(method.getName())) {
                throw new InvalidSchemaException("Schema method names must be unique. Duplicate: " + method.getName());
            }
            if (!method.getDeclaringClass().equals(WithFormat.class)) {
                if (method.getParameterCount() != 1) {
                    throw new InvalidSchemaException("Schema methods must take exactly 1 argument. Method: " + method.getName());
                }
                if (reservedMethodNames.contains(method.getName())) {
                    throw new InvalidSchemaException("Schema method name is reserved for internal use. Name: " + method.getName());
                }
            }
            schemaNames.add(method.getName());
        }
        return Collections.unmodifiableList(schemaNames);
    }

    private Class internalGenerate(Class schemaClass, ClassLoader classLoader, LoggingFormatter loggingFormatter) {
        DynamicType.Builder builder = new ByteBuddy().subclass(Instance.class).implement(schemaClass);
        int schemaIndex = 0;
        for (Method method : schemaClass.getMethods()) {
            int thisIndex = loggingFormatter.indexForArgument(method.getName(), schemaIndex++);
            Implementation methodCall;
            if (method.getDeclaringClass().equals(WithFormat.class)) {
                methodCall = invoke(formattedAtIndexMethod)
                        .with(thisIndex)
                        .withArgument(0)
                        .withArgument(1)
                        .andThen(FixedValue.self());
            } else {
                methodCall = invoke(setValueAtIndexMethod)
                        .with(thisIndex)
                        .withArgument(0)
                        .andThen(FixedValue.self());
            }
            builder = builder.method(named(method.getName())).intercept(methodCall);
        }
        return builder.make().load(classLoader).getLoaded();
    }
}
