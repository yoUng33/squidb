/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.processor.plugins;

import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.squidb.processor.data.ModelSpec;
import com.yahoo.squidb.processor.plugins.defaults.AndroidModelPlugin;
import com.yahoo.squidb.processor.plugins.defaults.ConstantCopyingPlugin;
import com.yahoo.squidb.processor.plugins.defaults.ConstructorPlugin;
import com.yahoo.squidb.processor.plugins.defaults.ErrorLoggingPlugin;
import com.yahoo.squidb.processor.plugins.defaults.ImplementsPlugin;
import com.yahoo.squidb.processor.plugins.defaults.JavadocPlugin;
import com.yahoo.squidb.processor.plugins.defaults.ModelMethodPlugin;
import com.yahoo.squidb.processor.plugins.defaults.enums.EnumPluginBundle;
import com.yahoo.squidb.processor.plugins.defaults.properties.InheritedModelSpecFieldPlugin;
import com.yahoo.squidb.processor.plugins.defaults.properties.TableModelSpecFieldPlugin;
import com.yahoo.squidb.processor.plugins.defaults.properties.ViewModelSpecFieldPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.SupportedOptions;
import javax.tools.Diagnostic;

/**
 * This class maintains a list of known/enabled {@link Plugin} classes. Plugins available by default include
 * {@link ConstructorPlugin} for generating model constructors, {@link ImplementsPlugin} for allowing models to
 * implement interfaces, {@link ModelMethodPlugin} for copying methods from the model spec to the model, and the three
 * property generator plugins ({@link TableModelSpecFieldPlugin}, {@link ViewModelSpecFieldPlugin}, and
 * {@link InheritedModelSpecFieldPlugin}).
 * <p>
 * This class also manages options for the default plugins--the default plugins for constructors, interfaces, and model
 * methods can all be disabled using an option. Other options allow disabling default content values, disabling the
 * convenience getters and setters that accompany each property, and preferring user-defined plugins to the default
 * plugins. Options are passed as a comma-separated list of strings to the processor using the key "squidbOptions".
 * <p>
 * All Plugin instances have access to the instance of PluginEnvironment that created them, and can call
 * {@link #hasOption(String)} or {@link #getEnvOptions()} to read any custom options specific to that plugin.
 */
public class PluginEnvironment {

    public static final String PLUGINS_KEY = "squidbPlugins";
    public static final String OPTIONS_KEY = "squidbOptions";
    private static final String SEPARATOR = ",";

    /**
     * Option for disabling the default constructors generated in each model class
     */
    public static final String OPTIONS_DISABLE_DEFAULT_CONSTRUCTORS = "disableDefaultConstructors";

    /**
     * Option for disabling default processing of the {@link com.yahoo.squidb.annotations.Implements} annotation for
     * declaring that models implement interfaces
     */
    public static final String OPTIONS_DISABLE_DEFAULT_IMPLEMENTS_HANDLING = "disableImplements";

    /**
     * Option for disabling the default copying of static methods and model methods from the spec to the model class
     */
    public static final String OPTIONS_DISABLE_DEFAULT_METHOD_HANDLING = "disableModelMethod";

    /**
     * Option for disabling the default copying of public static final fields as constants to the generated model
     */
    public static final String OPTIONS_DISABLE_DEFAULT_CONSTANT_COPYING = "disableConstantCopying";

    /**
     * Option for disabling the in-memory default content values used as for fallback values in empty models
     */
    public static final String OPTIONS_DISABLE_DEFAULT_VALUES = "disableDefaultValues";

    /**
     * Option for disabling the convenience getters and setters generated by default with each property
     */
    public static final String OPTIONS_DISABLE_DEFAULT_GETTERS_AND_SETTERS = "disableGettersAndSetters";

    /**
     * Option for disabling javadoc copying
     */
    public static final String OPTIONS_DISABLE_JAVADOC_COPYING = "disableJavadoc";

    /**
     * Option for disabling the default support for Enum properties
     */
    public static final String OPTIONS_DISABLE_ENUM_PROPERTIES = "disableEnumProperties";

    /**
     * Option for disabling the generated dummy classes used for error logging by the code generator, and instead
     * preferring the standard error logging provided by the APT APIs. Using standard error logging may cause the user
     * to see a large number of "cannot find symbol" errors in addition to the code generation errors logged by SquiDB,
     * so users should only use this option if they are having trouble with SquiDB's annotation-based error logging.
     */
    public static final String OPTIONS_USE_STANDARD_ERROR_LOGGING = "standardErrorLogging";

    /**
     * Option for generating models that have Android-specific features
     */
    public static final String OPTIONS_GENERATE_ANDROID_MODELS = "androidModels";

    private static final Set<String> SQUIDB_SUPPORTED_OPTIONS;

    static {
        SQUIDB_SUPPORTED_OPTIONS = new HashSet<>(9);
        SQUIDB_SUPPORTED_OPTIONS.add(OPTIONS_DISABLE_DEFAULT_CONSTRUCTORS);
        SQUIDB_SUPPORTED_OPTIONS.add(OPTIONS_DISABLE_DEFAULT_IMPLEMENTS_HANDLING);
        SQUIDB_SUPPORTED_OPTIONS.add(OPTIONS_DISABLE_DEFAULT_METHOD_HANDLING);
        SQUIDB_SUPPORTED_OPTIONS.add(OPTIONS_DISABLE_DEFAULT_CONSTANT_COPYING);
        SQUIDB_SUPPORTED_OPTIONS.add(OPTIONS_DISABLE_DEFAULT_VALUES);
        SQUIDB_SUPPORTED_OPTIONS.add(OPTIONS_DISABLE_DEFAULT_GETTERS_AND_SETTERS);
        SQUIDB_SUPPORTED_OPTIONS.add(OPTIONS_DISABLE_JAVADOC_COPYING);
        SQUIDB_SUPPORTED_OPTIONS.add(OPTIONS_DISABLE_ENUM_PROPERTIES);
        SQUIDB_SUPPORTED_OPTIONS.add(OPTIONS_GENERATE_ANDROID_MODELS);
    }

    private static final String UNSUPPORTED_OPTIONS_WARNING
            = "The following squidbOptions are not supported by SquiDB: [%s]. If you are using custom plugins that "
            + "inspect environment options, you should annotate those plugins with @javax.annotation.processing"
            + ".SupportedOptions and use your own environment option key; see PluginEnvironment.getEnvOptions() and "
            + "PluginEnvironment.hasEnvOption(String) for more information.";

    private final AptUtils utils;

    /** all environment options passed to APT */
    private final Map<String, String> envOptions;
    /** options passed as the value for the "squidbOptions" key */
    private final Set<String> squidbOptions;
    /** options keys supported by custom plugins */
    private final Set<String> pluginSupportedOptions;

    private List<Class<? extends Plugin>> highPriorityPlugins = new ArrayList<>();
    private List<Class<? extends Plugin>> normalPriorityPlugins = new ArrayList<>();
    private List<Class<? extends Plugin>> lowPriorityPlugins = new ArrayList<>();

    public enum PluginPriority {
        LOW,
        NORMAL,
        HIGH
    }

    /**
     * @param utils annotation processing utilities class
     * @param envOptions map of annotation processing options obtained from {@link ProcessingEnvironment#getOptions()}
     */
    public PluginEnvironment(AptUtils utils, Map<String, String> envOptions) {
        this.utils = utils;
        this.envOptions = Collections.unmodifiableMap(envOptions == null ? new HashMap<String, String>() : envOptions);
        this.squidbOptions = parseOptions();
        this.pluginSupportedOptions = new HashSet<>();

        initializeDefaultPlugins();
        initializePluginsFromEnvironment();
        reportUnsupportedOptions();
    }

    private Set<String> parseOptions() {
        Set<String> result = new HashSet<>();
        String optionsString = envOptions.get(OPTIONS_KEY);
        if (!AptUtils.isEmpty(optionsString)) {
            String[] allOptions = optionsString.split(SEPARATOR);
            Collections.addAll(result, allOptions);
        }
        return result;
    }

    private void initializeDefaultPlugins() {
        if (hasSquidbOption(OPTIONS_GENERATE_ANDROID_MODELS)) {
            normalPriorityPlugins.add(AndroidModelPlugin.class);
        }

        if (!hasSquidbOption(OPTIONS_DISABLE_DEFAULT_CONSTRUCTORS)) {
            normalPriorityPlugins.add(ConstructorPlugin.class);
        }
        if (!hasSquidbOption(OPTIONS_DISABLE_DEFAULT_IMPLEMENTS_HANDLING)) {
            normalPriorityPlugins.add(ImplementsPlugin.class);
        }
        if (!hasSquidbOption(OPTIONS_DISABLE_DEFAULT_METHOD_HANDLING)) {
            normalPriorityPlugins.add(ModelMethodPlugin.class);
        }
        if (!hasSquidbOption(OPTIONS_DISABLE_JAVADOC_COPYING)) {
            normalPriorityPlugins.add(JavadocPlugin.class);
        }

        // Can't disable these, but they can be overridden by user plugins with high priority
        normalPriorityPlugins.add(TableModelSpecFieldPlugin.class);
        normalPriorityPlugins.add(ViewModelSpecFieldPlugin.class);
        normalPriorityPlugins.add(InheritedModelSpecFieldPlugin.class);

        if (!hasSquidbOption(OPTIONS_DISABLE_ENUM_PROPERTIES)) {
            normalPriorityPlugins.add(EnumPluginBundle.class);
        }

        if (!hasSquidbOption(OPTIONS_USE_STANDARD_ERROR_LOGGING)) {
            normalPriorityPlugins.add(ErrorLoggingPlugin.class);
        }

        if (!hasSquidbOption(OPTIONS_DISABLE_DEFAULT_CONSTANT_COPYING)) {
            // This plugin claims any public static final fields not handled by the other plugins and copies them to
            // the generated model. Set to low priority so that by default user plugins can have first pass at
            // handing such fields.
            lowPriorityPlugins.add(ConstantCopyingPlugin.class);
        }
    }

    private void initializePluginsFromEnvironment() {
        String pluginsString = envOptions.get(PLUGINS_KEY);
        if (!AptUtils.isEmpty(pluginsString)) {
            String[] allPlugins = pluginsString.split(SEPARATOR);
            for (String plugin : allPlugins) {
                processPlugin(plugin);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void processPlugin(String pluginName) {
        try {
            PluginPriority priority = PluginPriority.NORMAL;
            if (pluginName.contains(":")) {
                String[] nameAndPriority = pluginName.split(":");
                if (nameAndPriority.length != 2) {
                    utils.getMessager().printMessage(Diagnostic.Kind.WARNING,
                            "Error parsing plugin and priority " + pluginName + ", plugin will be ignored");
                } else {
                    pluginName = nameAndPriority[0];
                    String priorityString = nameAndPriority[1];
                    try {
                        priority = PluginPriority.valueOf(priorityString.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        utils.getMessager().printMessage(Diagnostic.Kind.WARNING, "Unrecognized priority string " +
                                priorityString + " for plugin " + pluginName + ", defaulting to 'normal'. Should be " +
                                "one of '" + PluginPriority.HIGH + "', " + "'" + PluginPriority.NORMAL + "', or '" +
                                PluginPriority.LOW + "'.");
                        priority = PluginPriority.NORMAL;
                    }
                }
            }
            Class<?> pluginClass = Class.forName(pluginName);
            if (Plugin.class.isAssignableFrom(pluginClass)) {
                addPlugin((Class<? extends Plugin>) pluginClass, priority);
            } else {
                utils.getMessager().printMessage(Diagnostic.Kind.WARNING,
                        "Plugin " + pluginName + " is not a subclass of Plugin");
            }
        } catch (Exception e) {
            utils.getMessager().printMessage(Diagnostic.Kind.WARNING, "Unable to instantiate plugin " + pluginName +
                    ", reason: " + e);
        }
    }

    /**
     * Add a {@link Plugin} class to the list of known plugins
     *
     * @param plugin the plugin class
     * @param priority the priority to give the plugin
     */
    private void addPlugin(Class<? extends Plugin> plugin, PluginPriority priority) {
        switch (priority) {
            case LOW:
                lowPriorityPlugins.add(plugin);
                break;
            case HIGH:
                highPriorityPlugins.add(plugin);
                break;
            case NORMAL:
            default:
                normalPriorityPlugins.add(plugin);
                break;
        }

        SupportedOptions supportedOptionsAnnotation = plugin.getAnnotation(SupportedOptions.class);
        if (supportedOptionsAnnotation != null) {
            String[] options = supportedOptionsAnnotation.value();
            Collections.addAll(pluginSupportedOptions, options);
        }
    }

    private void reportUnsupportedOptions() {
        Set<String> unsupportedOptions = new HashSet<>(squidbOptions);
        unsupportedOptions.removeAll(SQUIDB_SUPPORTED_OPTIONS);

        if (AptUtils.isEmpty(unsupportedOptions)) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (String option : unsupportedOptions) {
            if (sb.length() > 0) {
                sb.append(SEPARATOR);
            }
            sb.append(option);
        }
        String message = String.format(UNSUPPORTED_OPTIONS_WARNING, sb);
        utils.getMessager().printMessage(Diagnostic.Kind.WARNING, message);
    }

    /**
     * Returns whether the value of the "squidbOptions" environment option contains the given string
     *
     * @param option the option to check
     * @return true if the option is set, false otherwise
     */
    public boolean hasSquidbOption(String option) {
        return squidbOptions.contains(option);
    }

    /**
     * Returns the map of annotation processing options obtained from {@link ProcessingEnvironment#getOptions()}.
     * Plugins may access this map to check for their own custom options.
     * <p>
     * Note: Custom plugins should also be annotated with {@link SupportedOptions @SupportedOptions} to declare which
     * environment options keys they support; these will be reported to the toolchain accordingly.
     *
     * @return the map of annotation processing options
     * @see #hasEnvOption(String)
     * @see #getEnvOptionValue(String)
     */
    public Map<String, String> getEnvOptions() {
        return envOptions;
    }

    /**
     * Returns whether the environment options obtained from {@link ProcessingEnvironment#getOptions()} contains the
     * given key. This is a convenience for plugins which declare their own options key but only need to check that the
     * key is present, rather than checking the value for that key.
     * <p>
     * Note: Custom plugins should also be annotated with {@link SupportedOptions @SupportedOptions} to declare which
     * environment options keys they support; these will be reported to the toolchain accordingly.
     *
     * @return true if the annotation processing options contains the given key, false otherwise
     */
    public boolean hasEnvOption(String key) {
        return envOptions.containsKey(key);
    }

    /**
     * Returns the value of the given option key from the environment options obtained from
     * {@link ProcessingEnvironment#getOptions()}. Returns null if the key is not present or if the associated value
     * is null. This is a convenience for plugins which declare their own options key.
     *
     * @return the value of the given environment option key
     */
    public String getEnvOptionValue(String key) {
        return envOptions.get(key);
    }

    /**
     * Return the set of environment options keys supported by custom plugins. This does not include the
     * <code>squidbPlugins</code> or <code>squidbOptions</code> keys, which are reserved for SquiDB use.
     *
     * @return the environment options keys supported by custom plugins
     */
    public Set<String> getPluginSupportedOptions() {
        return Collections.unmodifiableSet(pluginSupportedOptions);
    }

    /**
     * @return an AptUtils instance that provides useful annotation processing utility methods
     */
    public AptUtils getUtils() {
        return utils;
    }

    /**
     * @param modelSpec the model spec the Plugins will be instantiated for
     * @return a new {@link PluginBundle} containing Plugins initialized to handle the given model spec
     */
    public PluginBundle getPluginBundleForModelSpec(ModelSpec<?, ?> modelSpec) {
        List<Plugin> plugins = new ArrayList<>();
        accumulatePlugins(plugins, highPriorityPlugins, modelSpec);
        accumulatePlugins(plugins, normalPriorityPlugins, modelSpec);
        accumulatePlugins(plugins, lowPriorityPlugins, modelSpec);
        return new PluginBundle(modelSpec, this, plugins);
    }

    private void accumulatePlugins(List<Plugin> accumulator, List<Class<? extends Plugin>> pluginList,
            ModelSpec<?, ?> modelSpec) {
        for (Class<? extends Plugin> plugin : pluginList) {
            try {
                accumulator.add(plugin.getConstructor(ModelSpec.class, PluginEnvironment.class)
                        .newInstance(modelSpec, this));
            } catch (Exception e) {
                utils.getMessager().printMessage(Diagnostic.Kind.WARNING,
                        "Unable to instantiate plugin " + plugin + ", reason: " + e);
            }
        }
    }
}
