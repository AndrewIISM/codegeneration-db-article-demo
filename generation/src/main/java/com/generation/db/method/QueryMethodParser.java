package com.generation.db.method;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;

public class QueryMethodParser {

    public static List<QueryMethodParameter> parse(ExecutableElement queryMethod,
                                                   ExecutableType queryMethodType) {

        List<? extends VariableElement> variableParameters = queryMethod.getParameters();
        List<? extends TypeMirror> typeParameters = queryMethodType.getParameterTypes();

        List<QueryMethodParameter> queryMethodParameters = new ArrayList<>(variableParameters.size());

        for (int i = 0; i < variableParameters.size(); i++) {
            TypeMirror typeParameter = typeParameters.get(i);
            String nameParameter = variableParameters.get(i).getSimpleName().toString();

            QueryMethodParameter queryMethodParameter =
                    new QueryMethodParameter(nameParameter, typeParameter);

            queryMethodParameters.add(queryMethodParameter);
        }

        return queryMethodParameters;
    }

}
