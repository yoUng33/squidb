/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.writers;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.yahoo.squidb.processor.StringUtils;
import com.yahoo.squidb.processor.TypeConstants;
import com.yahoo.squidb.processor.data.TableModelSpecWrapper;
import com.yahoo.squidb.processor.plugins.PluginEnvironment;
import com.yahoo.squidb.processor.plugins.defaults.properties.generators.interfaces.TableModelPropertyGenerator;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

public class TableModelFileWriter extends ModelFileWriter<TableModelSpecWrapper> {

    public static final String TABLE_NAME = "TABLE";
    public static final String TABLE_MODEL_NAME = "TABLE_MODEL_NAME";

    public TableModelFileWriter(TypeElement element, PluginEnvironment pluginEnv) {
        super(new TableModelSpecWrapper(element, pluginEnv), pluginEnv);
    }

    @Override
    protected void declareModelSpecificFields() {
        String lastTableArg;
        if (modelSpec.isVirtualTable()) {
            lastTableArg = modelSpec.getSpecAnnotation().virtualModule();
        } else if (!StringUtils.isEmpty(modelSpec.getSpecAnnotation().tableConstraint())) {
            lastTableArg = modelSpec.getSpecAnnotation().tableConstraint();
        } else {
            lastTableArg = null;
        }

        FieldSpec.Builder tableField = FieldSpec.builder(modelSpec.getTableType(), TABLE_NAME,
                Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("new $T($T.class, $L, $S, null, $S)", modelSpec.getTableType(), modelSpec.getGeneratedClassName(),
                        PROPERTIES_ARRAY_NAME, modelSpec.getSpecAnnotation().tableName().trim(), lastTableArg);
        builder.addField(tableField.build());

        FieldSpec.Builder tableModelField = FieldSpec.builder(TypeConstants.TABLE_MODEL_NAME, TABLE_MODEL_NAME,
                Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("new $T($T.class, $L.getName())", TypeConstants.TABLE_MODEL_NAME,
                        modelSpec.getGeneratedClassName(), TABLE_NAME);
        builder.addField(tableModelField.build());
    }

    @Override
    protected void declareAllProperties() {
        for (TableModelPropertyGenerator generator : modelSpec.getPropertyGenerators()) {
            FieldSpec.Builder propertyBuilder = generator.buildTablePropertyDeclaration(TABLE_MODEL_NAME);
            modelSpec.getPluginBundle().willDeclareProperty(builder, generator, propertyBuilder);
            FieldSpec property = propertyBuilder.build();
            builder.addField(property);
            modelSpec.getPluginBundle().didDeclareProperty(builder, generator, property);
        }

        for (TableModelPropertyGenerator deprecatedGenerator : modelSpec.getDeprecatedPropertyGenerators()) {
            FieldSpec.Builder propertyBuilder = deprecatedGenerator.buildTablePropertyDeclaration(TABLE_MODEL_NAME);
            modelSpec.getPluginBundle().willDeclareProperty(builder, deprecatedGenerator, propertyBuilder);
            FieldSpec property = propertyBuilder.build();
            builder.addField(property);
            modelSpec.getPluginBundle().didDeclareProperty(builder, deprecatedGenerator, property);
        }
    }

    @Override
    protected void buildPropertiesInitializationBlock(CodeBlock.Builder block) {
        for (int i = 0; i < modelSpec.getPropertyGenerators().size(); i++) {
            block.addStatement("$L[$L] = $L", PROPERTIES_ARRAY_NAME, i,
                    modelSpec.getPropertyGenerators().get(i).getPropertyName());
        }
    }

    @Override
    protected void buildDefaultValuesInitializationBlock(CodeBlock.Builder block) {
        for (TableModelPropertyGenerator generator : modelSpec.getPropertyGenerators()) {
            CodeBlock putDefault = generator.buildPutDefault(DEFAULT_VALUES_NAME);
            if (putDefault != null && !putDefault.isEmpty()) {
                block.addStatement("$L", putDefault);
            }
        }
    }
}
