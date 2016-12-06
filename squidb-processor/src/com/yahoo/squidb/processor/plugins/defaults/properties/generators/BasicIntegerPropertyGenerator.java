/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.properties.generators;

import com.yahoo.aptutils.model.CoreTypes;
import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.ModelSpec;

import java.util.Arrays;
import java.util.List;

import javax.lang.model.element.VariableElement;

/**
 * An implementation of
 * {@link com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.TableModelPropertyGenerator}
 * for handling int fields
 */
public class BasicIntegerPropertyGenerator extends BasicTableModelPropertyGenerator {

    public static List<DeclaredTypeName> handledColumnTypes() {
        return Arrays.asList(CoreTypes.JAVA_BYTE, CoreTypes.JAVA_SHORT, CoreTypes.JAVA_INTEGER,
                CoreTypes.PRIMITIVE_BYTE, CoreTypes.PRIMITIVE_SHORT, CoreTypes.PRIMITIVE_INT);
    }

    public BasicIntegerPropertyGenerator(ModelSpec<?, ?> modelSpec, String columnName, AptUtils utils) {
        super(modelSpec, columnName, utils);
    }

    public BasicIntegerPropertyGenerator(ModelSpec<?, ?> modelSpec, String columnName,
            String propertyName, AptUtils utils) {
        super(modelSpec, columnName, propertyName, utils);
    }

    public BasicIntegerPropertyGenerator(ModelSpec<?, ?> modelSpec, VariableElement field, AptUtils utils) {
        super(modelSpec, field, utils);
    }

    @Override
    public DeclaredTypeName getTypeForAccessors() {
        return CoreTypes.JAVA_INTEGER;
    }

    @Override
    public DeclaredTypeName getPropertyType() {
        return TypeConstants.INTEGER_PROPERTY;
    }

}
