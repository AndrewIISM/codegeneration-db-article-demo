package com.generation.db;

import com.generation.db.annotation.Query;
import com.generation.db.generator.JdbcRepositoryQueryMethodGenerator;
import com.generation.db.method.QueryMethodParameter;
import com.generation.db.method.QueryMethodParser;
import com.generation.db.query.QueryProcessor;
import com.generation.db.query.QueryWithParameters;
import com.squareup.javapoet.MethodSpec;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.util.Types;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class QueryMethodGenerator {

    public static List<MethodSpec> generateQueryMethods(Element repositoryElement,
                                                        ProcessingEnvironment processingEnvironment) {
        Types typeUtils = processingEnvironment.getTypeUtils();


        List<MethodSpec> generatedQueryMethods = new ArrayList<>();

        List<ExecutableElement> queryMethods = findMethodsWithQueryAnnotation(repositoryElement);

        DeclaredType repositoryType = (DeclaredType) repositoryElement.asType();
        for (ExecutableElement queryMethod : queryMethods) {
            ExecutableType queryMethodType =
                    (ExecutableType) typeUtils.asMemberOf(repositoryType, queryMethod);

            AnnotationMirror queryAnnotation = findAnnotation(queryMethod, Query.class).get();
            String queryPath = getAnnotationValue(queryAnnotation);

            List<QueryMethodParameter> queryMethodParameters = QueryMethodParser.parse(queryMethod, queryMethodType);
            QueryWithParameters queryWithParameters =
                    QueryProcessor.process(processingEnvironment.getFiler(), queryPath, queryMethodParameters);

            MethodSpec generatedQueryMethod =
                    JdbcRepositoryQueryMethodGenerator.generate(queryMethod, queryMethodType, queryWithParameters);

            generatedQueryMethods.add(generatedQueryMethod);
        }

        return generatedQueryMethods;
    }

    public static List<ExecutableElement> findMethodsWithQueryAnnotation(Element repositoryElement) {
        return repositoryElement.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.METHOD)
                .filter(e -> e.getModifiers().contains(Modifier.ABSTRACT))
                .filter(e -> findAnnotation(e, Query.class).isPresent())
                .map(ExecutableElement.class::cast)
                .toList();
    }

    public static Optional<AnnotationMirror> findAnnotation(Element abstractMethodElement,
                                                            Class<? extends Annotation> annotationClass) {

        for (AnnotationMirror annotationMirror : abstractMethodElement.getAnnotationMirrors()) {
            if (annotationMirror.getAnnotationType().toString().equals(annotationClass.getCanonicalName())) {
                return Optional.of(annotationMirror);
            }
        }

        return Optional.empty();
    }

    private static String getAnnotationValue(AnnotationMirror annotation) {
        Map<? extends ExecutableElement, ? extends AnnotationValue> annotationElementValues = annotation.getElementValues();
        for (var entry : annotationElementValues.entrySet()) {
            if (entry.getKey().getSimpleName().toString().equals("value")) {
                return (String) entry.getValue().getValue();
            }
        }

        throw new RuntimeException("Annotation " + annotation + " doesn't contain value");
    }

}
