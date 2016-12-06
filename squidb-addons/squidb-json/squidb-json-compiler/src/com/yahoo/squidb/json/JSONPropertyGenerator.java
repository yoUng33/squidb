/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.json;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.aptutils.writer.JavaFileWriter;
import com.yahoo.aptutils.writer.parameters.MethodDeclarationParameters;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.BasicStringPropertyGenerator;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import javax.lang.model.element.VariableElement;

public class JSONPropertyGenerator extends BasicStringPropertyGenerator {

    private final DeclaredTypeName fieldType;
    private final JSONPropertyGeneratorDelegate delegate;

    public JSONPropertyGenerator(ModelSpec<?, ?> modelSpec, VariableElement field, DeclaredTypeName fieldType,
            AptUtils utils) {
        super(modelSpec, field, utils);
        this.fieldType = fieldType;
        this.delegate = new JSONPropertyGeneratorDelegate(getPropertyName(), getTypeForAccessors());
    }

    @Override
    public void registerRequiredImports(Set<DeclaredTypeName> imports) {
        super.registerRequiredImports(imports);
        delegate.registerRequiredImports(imports);
    }

    @Override
    public DeclaredTypeName getPropertyType() {
        DeclaredTypeName jsonProperty = JSONTypes.JSON_PROPERTY.clone();
        jsonProperty.setTypeArgs(Collections.singletonList(fieldType));
        return jsonProperty;
    }

    @Override
    public DeclaredTypeName getTypeForAccessors() {
        return fieldType;
    }

    @Override
    protected void writeGetterBody(JavaFileWriter writer, MethodDeclarationParameters params) throws IOException {
        delegate.writeGetterBody(writer, params);
    }

    @Override
    protected void writeSetterBody(JavaFileWriter writer, MethodDeclarationParameters params) throws IOException {
        delegate.writeSetterBody(writer, params);
    }
}
