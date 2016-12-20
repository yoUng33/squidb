/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.properties.generators;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.aptutils.writer.JavaFileWriter;
import com.yahoo.aptutils.writer.parameters.MethodDeclarationParameters;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import javax.lang.model.element.VariableElement;

/**
 * Property generator for Enum fields in a model spec.
 */
public class EnumPropertyGenerator extends BasicStringPropertyGenerator {

    private final DeclaredTypeName enumType;
    private final EnumPropertyGeneratorDelegate delegate;

    public EnumPropertyGenerator(ModelSpec<?, ?> modelSpec, VariableElement field, AptUtils utils,
            DeclaredTypeName enumType) {
        super(modelSpec, field, utils);
        this.enumType = enumType;
        this.delegate = new EnumPropertyGeneratorDelegate(getPropertyName(), getTypeForAccessors());
    }

    @Override
    public void registerRequiredImports(Set<DeclaredTypeName> imports) {
        super.registerRequiredImports(imports);
        delegate.registerRequiredImports(imports);
    }

    @Override
    public DeclaredTypeName getPropertyType() {
        DeclaredTypeName enumProperty = TypeConstants.ENUM_PROPERTY.clone();
        enumProperty.setTypeArgs(Collections.singletonList(enumType));
        return enumProperty;
    }

    @Override
    public DeclaredTypeName getTypeForAccessors() {
        return enumType;
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
