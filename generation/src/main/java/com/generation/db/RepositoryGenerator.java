package com.generation.db;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import java.io.IOException;
import java.util.List;

public class RepositoryGenerator {

    private final ProcessingEnvironment processingEnvironment;

    public RepositoryGenerator(ProcessingEnvironment processingEnvironment) {
        this.processingEnvironment = processingEnvironment;
    }

    public void generate(Element repositoryElement) {
        String nameOfGeneratingClass = "$" + repositoryElement.getSimpleName() + "Impl";

        MethodSpec.Builder repositoryConstructorBuilder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC);

        TypeSpec.Builder repositoryClassBuilder = TypeSpec.classBuilder(nameOfGeneratingClass)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(repositoryElement.asType())
                .addOriginatingElement(repositoryElement);

        addConnectionManager(repositoryClassBuilder, repositoryConstructorBuilder);

        // The main method which generate query methods and analyze code
        List<MethodSpec> generatedMethods =
                QueryMethodGenerator.generateQueryMethods(repositoryElement, processingEnvironment);

        repositoryClassBuilder
                .addMethod(repositoryConstructorBuilder.build())
                .addMethods(generatedMethods);

        saveGeneratedClass(repositoryClassBuilder.build(), repositoryElement);
    }

    private void addConnectionManager(TypeSpec.Builder repositoryClassBuilder,
                                      MethodSpec.Builder repositoryConstructorBuilder) {

        ClassName connectionManger = ClassName.get(ConnectionManager.class);

        repositoryClassBuilder
                .addField(connectionManger, "_connectionManger", Modifier.PRIVATE, Modifier.FINAL);

        repositoryConstructorBuilder
                .addParameter(connectionManger, "_connectionManger")
                .addStatement("this._connectionManger = _connectionManger");
    }

    private void saveGeneratedClass(TypeSpec repositorySpec, Element repositoryElement) {
        try {
            PackageElement packageElement = this.processingEnvironment.getElementUtils().getPackageOf(repositoryElement);
            String packageName = packageElement.getQualifiedName().toString();

            JavaFile javaFile = JavaFile.builder(packageName, repositorySpec).build();
            javaFile.writeTo(processingEnvironment.getFiler());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
