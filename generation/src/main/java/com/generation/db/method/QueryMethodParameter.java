package com.generation.db.method;

import javax.lang.model.type.TypeMirror;

public record QueryMethodParameter(
        String name,
        TypeMirror type
) { }
