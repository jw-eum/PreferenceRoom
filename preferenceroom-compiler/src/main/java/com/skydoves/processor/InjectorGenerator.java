/*
 * Copyright (C) 2017 skydoves
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

package com.skydoves.processor;

import android.support.annotation.NonNull;

import com.google.common.base.VerifyException;
import com.skydoves.preferenceroom.InjectPreference;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;

import static javax.lang.model.element.Modifier.PUBLIC;

public class InjectorGenerator {

    private static final String CLAZZ_PREFIX = "_Injector";
    private static final String INJECT_OBJECT = "injectObject";
    private static final String PREFERENCE_PREFIX = "Preference_";
    private static final String COMPONENT_PREFIX = "PreferenceComponent_";

    private final PreferenceComponentAnnotatedClass annotatedClazz;
    private final TypeElement injectedElement;
    public String packageName;

    public InjectorGenerator(@NonNull PreferenceComponentAnnotatedClass annotatedClass, @NonNull TypeElement injectedElement, @NonNull Elements elementUtils) {
        this.annotatedClazz = annotatedClass;
        this.injectedElement = injectedElement;
        PackageElement packageElement = elementUtils.getPackageOf(injectedElement);
        this.packageName = packageElement.isUnnamed() ? null : packageElement.getQualifiedName().toString();
    }

    public TypeSpec generate() {
        return TypeSpec.classBuilder(getClazzName())
                .addJavadoc("Generated by PreferenceRoom. (https://github.com/skydoves/PreferenceRoom).\n")
                .addModifiers(PUBLIC)
                .addMethod(getConstructorSpec())
                .build();
    }

    public MethodSpec getConstructorSpec() {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addModifiers(PUBLIC)
                .addParameter(ParameterSpec.builder(TypeName.get(injectedElement.asType()), INJECT_OBJECT).addAnnotation(NonNull.class).build());

        injectedElement.getEnclosedElements().stream()
                .filter(variable -> variable instanceof VariableElement)
                .map(variable -> (VariableElement) variable)
                .forEach(variable -> {
                    if(variable.getAnnotation(InjectPreference.class) != null) {
                        String annotatedFieldName = TypeName.get(variable.asType()).toString();
                        if(annotatedFieldName.contains(".") || annotatedFieldName.contains("\\.")) {
                            ByteBuffer byteBuffer = Charset.forName("UTF-8").encode(annotatedFieldName);
                            String arr = StandardCharsets.UTF_8.decode(byteBuffer).toString();
                            String[] typedArray = arr.split("\\.");
                            annotatedFieldName = typedArray[typedArray.length-1];
                        }

                        ClassName componentClazz = ClassName.get(annotatedClazz.packageName, COMPONENT_PREFIX + annotatedClazz.clazzName);
                        if(annotatedClazz.generatedClazzList.contains(annotatedFieldName)) {
                            builder.addStatement(INJECT_OBJECT + ".$N = $T.getInstance().$N()",
                                    variable.getSimpleName(), componentClazz, annotatedFieldName.replace(PREFERENCE_PREFIX, ""));
                        } else if((COMPONENT_PREFIX + annotatedClazz.clazzName).equals(annotatedFieldName)) {
                            builder.addStatement(INJECT_OBJECT + ".$N = $T.getInstance()",
                                    variable.getSimpleName(), componentClazz);
                        } else {
                            throw new VerifyException(String.format("'%s' type can not be injected!!!", annotatedFieldName));
                        }
                    }
                });

        return builder.build();
    }

    private String getClazzName() {
        return injectedElement.getSimpleName() + CLAZZ_PREFIX;
    }
}
