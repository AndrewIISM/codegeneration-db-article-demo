package com.generation.db.query;

import com.generation.db.method.QueryMethodParameter;

import java.util.List;
import java.util.Map;

public record QueryWithParameters(
        String executableQuery,
        Map<QueryMethodParameter, List<Integer>> parametersWithIndexes
) { }
