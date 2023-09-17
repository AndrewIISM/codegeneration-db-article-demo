package com.generation.db.generator;

import com.generation.db.generator.JdbcTypes.JdbcTypeMethods;
import com.generation.db.method.QueryMethodParameter;
import com.generation.db.query.QueryWithParameters;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class JdbcRepositoryQueryMethodGenerator {

    public static MethodSpec generate(ExecutableElement queryMethod,
                                      ExecutableType queryMethodType,
                                      QueryWithParameters queryWithParameters) {

        MethodSpec.Builder methodBuilder = getMethodBuilder(queryMethod, queryMethodType);

        addQueryVariable(queryWithParameters.executableQuery(), methodBuilder);
        addOpeningTryBlockWithConnection(methodBuilder);
        addFillingStatementBasedOnQueryParameters(queryWithParameters.parametersWithIndexes(), methodBuilder);
        addStatementExecutionWithMapper(queryMethod, methodBuilder);
        addClosingTryCatchCode(methodBuilder);

        return methodBuilder.build();
    }

    private static MethodSpec.Builder getMethodBuilder(ExecutableElement queryMethod, ExecutableType queryMethodType) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(queryMethod.getSimpleName().toString())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.get(queryMethodType.getReturnType()));

        // Add parameters
        for (int i = 0; i < queryMethod.getParameters().size(); i++) {
            VariableElement parameter = queryMethod.getParameters().get(i);
            TypeMirror parameterType = queryMethodType.getParameterTypes().get(i);
            String name = parameter.getSimpleName().toString();

            ParameterSpec.Builder parameterBuilder = ParameterSpec.builder(TypeName.get(parameterType), name);

            methodBuilder.addParameter(parameterBuilder.build());
        }

        return methodBuilder;
    }

    private static void addQueryVariable(String query, MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("var _query = $S", query);
    }

    private static void addOpeningTryBlockWithConnection(MethodSpec.Builder methodBuilder) {
        methodBuilder.addCode("""
            try (var _connection = this._connectionManger.getConnection();
                         var _stmt = _connection.prepareStatement(_query)) {$>
            """);
    }

    record ParameterWithIndex(QueryMethodParameter parameter, Integer index) {};

    private static void addFillingStatementBasedOnQueryParameters(Map<QueryMethodParameter, List<Integer>> queryMethodParameterWithIndexes,
                                                                  MethodSpec.Builder methodBuilder) {

        List<ParameterWithIndex> parametersWithIndex = new ArrayList<>();
        queryMethodParameterWithIndexes.forEach((parameter, queryIndexes) -> {
            for (Integer queryIndex : queryIndexes) {
                parametersWithIndex.add(new ParameterWithIndex(parameter, queryIndex));
            }
        });

        parametersWithIndex.sort(Comparator.comparingInt(ParameterWithIndex::index));

        for (int i = 0; i < parametersWithIndex.size(); i++) {
            ParameterWithIndex parameterWithIndex = parametersWithIndex.get(i);
            QueryMethodParameter parameter = parameterWithIndex.parameter;

            JdbcTypeMethods jdbcTypeMethods =
                    JdbcTypes.jdbcTypesWithMethods.get(TypeName.get(parameter.type()));

            methodBuilder.addStatement("_stmt.$L", jdbcTypeMethods.set().apply(i + 1, parameter.name()));
        }

    }

    private static void addStatementExecutionWithMapper(ExecutableElement queryMethod,
                                                        MethodSpec.Builder methodBuilder) {

        TypeMirror returnType = queryMethod.getReturnType();

        JdbcTypeMethods jdbcTypeMethods =
                JdbcTypes.jdbcTypesWithMethods.get(TypeName.get(returnType));

        if (isVoid(returnType)) {
            methodBuilder.addStatement("_stmt.execute()");
            return;
        }

        methodBuilder.addCode("""
                try(var _resultSet = _stmt.executeQuery()) {
                    if (!_resultSet.next()) {
                        return null;
                    }
                    
                    return _resultSet.$L;
                }
                """, jdbcTypeMethods.get().apply(CodeBlock.of("1")));
    }

    private static boolean isVoid(TypeMirror returnType) {
        final String typeAsStr = returnType.toString();

        return returnType.getKind().equals(TypeKind.VOID)
                || Void.class.getCanonicalName().equals(typeAsStr)
                || "void".equals(typeAsStr);
    }

    private static void addClosingTryCatchCode(MethodSpec.Builder methodBuilder) {
        methodBuilder.addCode("""
                $<} catch (Exception e) {
                  throw new RuntimeException(e);
                }""");
    }

}
