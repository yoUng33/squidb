/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins.defaults.enums;

import com.squareup.javapoet.TypeName;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.TableModelSpecWrapper;
import com.yahoo.squidb.processor.plugins.defaults.properties.BaseFieldPlugin;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.EnumPropertyGenerator;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.TableModelPropertyGenerator;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

/**
 * Plugin which handles Enum fields in a TableModelSpec file.
 */
public class EnumFieldPlugin extends BaseFieldPlugin<TableModelSpecWrapper, TableModelPropertyGenerator> {

    @Override
    protected Class<TableModelSpecWrapper> getHandledModelSpecClass() {
        return TableModelSpecWrapper.class;
    }

    @Override
    protected boolean hasPropertyGeneratorForField(VariableElement field, TypeName fieldType) {
        if (TypeConstants.isConstant(field)) {
            // Looks like a constant, ignore
            return false;
        }

        TypeElement typeElement = (TypeElement) pluginEnv.getProcessingEnvironment()
                .getTypeUtils().asElement(field.asType());
        return typeElement != null && typeElement.getKind() == ElementKind.ENUM;
    }

    @Override
    protected TableModelPropertyGenerator getPropertyGenerator(VariableElement field, TypeName fieldType) {
        return new EnumPropertyGenerator(modelSpec, field, pluginEnv, fieldType);
    }
}

