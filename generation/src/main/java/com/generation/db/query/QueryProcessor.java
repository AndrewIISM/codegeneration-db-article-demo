package com.generation.db.query;

import com.generation.db.method.QueryMethodParameter;

import javax.annotation.processing.Filer;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryProcessor {

    public static QueryWithParameters process(Filer filer, String queryPath, List<QueryMethodParameter> parameters) {
        String rawQuery = getRawQuery(filer, queryPath);

        List<QueryMethodParameter> sortedParametersByNameDesc = parameters.stream()
                .sorted(Comparator.comparing(QueryMethodParameter::name).reversed())
                .toList();

        Map<QueryMethodParameter, List<Integer>> queryMethodParameterWithIndexes = new HashMap<>();
        for (QueryMethodParameter parameter : sortedParametersByNameDesc) {
            String parameterName = parameter.name();
            List<Integer> queryIndexes = parseQueryParameter(rawQuery, parameterName);

            queryMethodParameterWithIndexes.put(parameter, queryIndexes);
        }

        List<String> parameterNames = sortedParametersByNameDesc.stream()
                .map(QueryMethodParameter::name)
                .toList();

        String executableQuery = makeExecutableQuery(rawQuery, parameterNames);

        return new QueryWithParameters(executableQuery, queryMethodParameterWithIndexes);
    }

    private static String getRawQuery(Filer filer, String queryPath) {
        try (InputStream is = filer.getResource(StandardLocation.CLASS_PATH, "", queryPath).openInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("SQL file wasn't found in CLASSPATH by path: " + queryPath, e);
        }
    }

    private static List<Integer> parseQueryParameter(String rawQuery, String parameterName) {
        List<Integer> sqlIndexes = new ArrayList<>();

        int index = -1;
        while ((index = rawQuery.indexOf(":" + parameterName, index + 1)) >= 0) {
            sqlIndexes.add(index);
        }

        return sqlIndexes;
    }

    private static String makeExecutableQuery(String rawQuery, List<String> parameterNames) {
        String executableQuery = rawQuery;

        for (String parameterName : parameterNames) {
            executableQuery = executableQuery.replace(":" + parameterName, "?");
        }

        return executableQuery;
    }

}
