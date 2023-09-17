package com.generation.db.generator;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public class JdbcTypes {

    public record JdbcTypeMethods(
            Function<CodeBlock, CodeBlock> get,
            BiFunction<Integer, String, CodeBlock> set
    ) {}

    public static Map<TypeName, JdbcTypeMethods> jdbcTypesWithMethods = new HashMap<>();

    static {
        jdbcTypesWithMethods.put(
                TypeName.INT,
                new JdbcTypeMethods(
                        index -> CodeBlock.of("getInt($L)", index),
                        (idxOrName, variableName) -> CodeBlock.of("setInt($L, $L)", idxOrName, variableName)
                )
        );
        jdbcTypesWithMethods.put(
                TypeName.get(String.class),
                new JdbcTypeMethods(
                        index -> CodeBlock.of("getString($L)", index),
                        (idxOrName, variableName) -> CodeBlock.of("setString($L, $L)", idxOrName, variableName)
                )
        );
        jdbcTypesWithMethods.put(
                TypeName.get(BigDecimal.class),
                new JdbcTypeMethods(
                        index -> CodeBlock.of("getObject($L, $T)", index, BigDecimal.class),
                        (idxOrName, variableName) -> CodeBlock.of("setObject($L, $L, $T.NUMERIC)", idxOrName, variableName, java.sql.Types.class)
                )
        );
    }

}
