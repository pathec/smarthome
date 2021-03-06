/**
 * Copyright (c) 1997, 2015 by ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.automation.internal.commands;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.eclipse.smarthome.automation.Rule;
import org.eclipse.smarthome.automation.RuleRegistry;
import org.eclipse.smarthome.automation.RuleStatus;
import org.eclipse.smarthome.automation.RuleStatusInfo;
import org.eclipse.smarthome.automation.template.RuleTemplate;
import org.eclipse.smarthome.automation.template.TemplateRegistry;
import org.eclipse.smarthome.automation.type.ActionType;
import org.eclipse.smarthome.automation.type.ConditionType;
import org.eclipse.smarthome.automation.type.ModuleType;
import org.eclipse.smarthome.automation.type.ModuleTypeRegistry;
import org.eclipse.smarthome.automation.type.TriggerType;
import org.eclipse.smarthome.io.console.Console;
import org.eclipse.smarthome.io.console.extensions.ConsoleCommandExtension;
import org.osgi.service.component.ComponentContext;

/**
 * This class provides functionality for defining and executing automation commands for importing, exporting, removing
 * and listing the automation objects.
 *
 * @author Ana Dimova - Initial Contribution
 * @author Kai Kreuzer - refactored (managed) provider and registry implementation
 *
 */
@SuppressWarnings("rawtypes")
public class AutomationCommandsPluggable extends AutomationCommands implements ConsoleCommandExtension {

    /**
     * This constant defines the command group name.
     */
    public static final String NAME = "automation";

    /**
     * This constant describes the commands group.
     */
    public static final String DESCRIPTION = "Commands for managing Automation Rules, Templates and ModuleTypes resources.";

    /**
     * This field holds the reference to the {@code RuleRegistry} providing the {@code Rule} automation objects.
     */
    static RuleRegistry ruleRegistry;

    /**
     * This field holds the reference to the {@code TemplateRegistry} providing the {@code Template} automation objects.
     */
    static TemplateRegistry<RuleTemplate> templateRegistry;

    /**
     * This field holds the reference to the {@code ModuleTypeRegistry} providing the {@code ModuleType} automation
     * objects.
     */
    static ModuleTypeRegistry moduleTypeRegistry;

    /**
     * This constant is defined for compatibility and is used to switch to a particular provider of {@code ModuleType}
     * automation objects.
     */
    private static final int MODULE_TYPE_REGISTRY = 3;

    /**
     * This constant is defined for compatibility and is used to switch to a particular provider of {@code Template}
     * automation objects.
     */
    private static final int TEMPLATE_REGISTRY = 2;

    /**
     * This constant is defined for compatibility and is used to switch to a particular provider of {@code Rule}
     * automation objects.
     */
    private static final int RULE_REGISTRY = 1;

    /**
     * Activating this component - called from DS.
     *
     * @param componentContext
     */
    protected void activate(ComponentContext componentContext) {
        super.initialize(componentContext.getBundleContext());
    }

    /**
     * Deactivating this component - called from DS.
     */
    protected void deactivate(ComponentContext componentContext) {
        super.dispose();
    }

    /**
     * Bind the {@link RuleRegistry} service - called from DS.
     *
     * @param ruleRegistry ruleRegistry service.
     */
    protected void setRuleRegistry(RuleRegistry ruleRegistry) {
        AutomationCommandsPluggable.ruleRegistry = ruleRegistry;
    }

    /**
     * Bind the {@link ModuleTypeRegistry} service - called from DS.
     *
     * @param moduleTypeRegistry moduleTypeRegistry service.
     */
    protected void setModuleTypeRegistry(ModuleTypeRegistry moduleTypeRegistry) {
        AutomationCommandsPluggable.moduleTypeRegistry = moduleTypeRegistry;
    }

    /**
     * Bind the {@link TemplateRegistry} service - called from DS.
     *
     * @param templateRegistry templateRegistry service.
     */
    protected void setTemplateRegistry(TemplateRegistry<RuleTemplate> templateRegistry) {
        AutomationCommandsPluggable.templateRegistry = templateRegistry;
    }

    protected void unsetRuleRegistry(RuleRegistry ruleRegistry) {
        AutomationCommandsPluggable.ruleRegistry = null;
    }

    protected void unsetModuleTypeRegistry(ModuleTypeRegistry moduleTypeRegistry) {
        AutomationCommandsPluggable.moduleTypeRegistry = null;
    }

    protected void unsetTemplateRegistry(TemplateRegistry templateRegistry) {
        AutomationCommandsPluggable.templateRegistry = null;
    }

    @Override
    public void execute(String[] args, Console console) {
        if (args.length == 0) {
            console.println(StringUtils.join(getUsages(), "\n"));
            return;
        }

        String command = args[0];// the first argument is the subcommand name

        String[] params = new String[args.length - 1];// extract the remaining arguments except the first one
        if (params.length > 0) {
            System.arraycopy(args, 1, params, 0, params.length);
        }

        String res = super.executeCommand(command, params);
        if (res == null) {
            console.println(String.format("Unsupported command %s", command));
        } else {
            console.println(res);
        }
    }

    @Override
    public List<String> getUsages() {
        return Arrays.asList(new String[] {
                buildCommandUsage(LIST_MODULE_TYPES + " [-st] <filter> <language>",
                        "lists all Module Types. If filter is present, lists only matching Module Types."
                                + " If language is missing, the default language will be used."),
                buildCommandUsage(LIST_TEMPLATES + " [-st] <filter> <language>",
                        "lists all Templates. If filter is present, lists only matching Templates."
                                + " If language is missing, the default language will be used."),
                buildCommandUsage(LIST_RULES + " [-st] <filter>",
                        "lists all Rules. If filter is present, lists only matching Rules"),
                buildCommandUsage(REMOVE_MODULE_TYPES + " [-st] <url>",
                        "Removes the Module Types, loaded from the given url"),
                buildCommandUsage(REMOVE_TEMPLATES + " [-st] <url>",
                        "Removes the Templates, loaded from the given url"),
                buildCommandUsage(REMOVE_RULE + " [-st] <uid>", "Removes the rule, specified by given UID"),
                buildCommandUsage(REMOVE_RULES + " [-st] <filter>",
                        "Removes the rules. If filter is present, removes only matching Rules"),
                buildCommandUsage(IMPORT_MODULE_TYPES + " [-p] <parserType> [-st] <url>",
                        "Imports Module Types from given url. If parser type missing, \"json\" parser will be set as default"),
                buildCommandUsage(IMPORT_TEMPLATES + " [-p] <parserType> [-st] <url>",
                        "Imports Templates from given url. If parser type missing, \"json\" parser will be set as default"),
                buildCommandUsage(IMPORT_RULES + " [-p] <parserType> [-st] <url>",
                        "Imports Rules from given url. If parser type missing, \"json\" parser will be set as default"),
                buildCommandUsage(EXPORT_MODULE_TYPES + " [-p] <parserType> [-st] <file>",
                        "Exports Module Types in a file. If parser type missing, \"json\" parser will be set as default"),
                buildCommandUsage(EXPORT_TEMPLATES + " [-p] <parserType> [-st] <file>",
                        "Exports Templates in a file. If parser type missing, \"json\" parser will be set as default"),
                buildCommandUsage(EXPORT_RULES + " [-p] <parserType> [-st] <file>",
                        "Exports Rules in a file. If parser type missing, \"json\" parser will be set as default"),
                buildCommandUsage(ENABLE_RULE + " [-st] <uid> <enable>",
                        "Enables the Rule, specified by given UID. If enable parameter is missing, "
                                + "the result of the command will be visualization of enabled/disabled state of the rule, "
                                + "if its value is \"true\" or \"false\", "
                                + "the result of the command will be to set enable/disable on the Rule.") });
    }

    @Override
    public String getCommand() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public Rule getRule(String uid) {
        if (ruleRegistry != null) {
            return ruleRegistry.get(uid);
        }
        return null;
    }

    @Override
    public RuleTemplate getTemplate(String templateUID, Locale locale) {
        if (templateRegistry != null) {
            return templateRegistry.get(templateUID, locale);
        }
        return null;
    }

    @Override
    public Collection<RuleTemplate> getTemplates(Locale locale) {
        if (templateRegistry != null) {
            return templateRegistry.getAll(locale);
        }
        return null;
    }

    @Override
    public ModuleType getModuleType(String typeUID, Locale locale) {
        if (moduleTypeRegistry != null) {
            return moduleTypeRegistry.get(typeUID, locale);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<TriggerType> getTriggers(Locale locale) {
        if (moduleTypeRegistry != null) {
            return moduleTypeRegistry.getTriggers(locale);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<ConditionType> getConditions(Locale locale) {
        if (moduleTypeRegistry != null) {
            return moduleTypeRegistry.getConditions(locale);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<ActionType> getActions(Locale locale) {
        if (moduleTypeRegistry != null) {
            return moduleTypeRegistry.getActions(locale);
        }
        return null;
    }

    @Override
    public String removeRule(String uid) {
        if (ruleRegistry != null) {
            if (ruleRegistry.remove(uid) != null) {
                return AutomationCommand.SUCCESS;
            } else {
                return String.format("Rule with id '%s' does not exist.", uid);
            }
        }
        return String.format("%s! RuleRegistry not available!", AutomationCommand.FAIL);
    }

    @Override
    public String removeRules(String ruleFilter) {
        if (ruleRegistry != null) {
            for (Rule r : ruleRegistry.getAll()) {
                if (r.getUID().contains(ruleFilter)) {
                    ruleRegistry.remove(r.getUID());
                }
            }
            return AutomationCommand.SUCCESS;
        }
        return String.format("%s! RuleRegistry not available!", AutomationCommand.FAIL);
    }

    @Override
    protected AutomationCommand parseCommand(String command, String[] params) {
        if (command.equalsIgnoreCase(IMPORT_MODULE_TYPES)) {
            return new AutomationCommandImport(IMPORT_MODULE_TYPES, params, MODULE_TYPE_REGISTRY, this);
        }
        if (command.equalsIgnoreCase(EXPORT_MODULE_TYPES)) {
            return new AutomationCommandExport(EXPORT_MODULE_TYPES, params, MODULE_TYPE_REGISTRY, this);
        }
        if (command.equalsIgnoreCase(LIST_MODULE_TYPES)) {
            return new AutomationCommandList(LIST_MODULE_TYPES, params, MODULE_TYPE_REGISTRY, this);
        }
        if (command.equalsIgnoreCase(IMPORT_TEMPLATES)) {
            return new AutomationCommandImport(IMPORT_TEMPLATES, params, TEMPLATE_REGISTRY, this);
        }
        if (command.equalsIgnoreCase(EXPORT_TEMPLATES)) {
            return new AutomationCommandExport(EXPORT_TEMPLATES, params, TEMPLATE_REGISTRY, this);
        }
        if (command.equalsIgnoreCase(LIST_TEMPLATES)) {
            return new AutomationCommandList(LIST_TEMPLATES, params, TEMPLATE_REGISTRY, this);
        }
        if (command.equalsIgnoreCase(IMPORT_RULES)) {
            return new AutomationCommandImport(IMPORT_RULES, params, RULE_REGISTRY, this);
        }
        if (command.equalsIgnoreCase(EXPORT_RULES)) {
            return new AutomationCommandExport(EXPORT_RULES, params, RULE_REGISTRY, this);
        }
        if (command.equalsIgnoreCase(LIST_RULES)) {
            return new AutomationCommandList(LIST_RULES, params, RULE_REGISTRY, this);
        }
        if (command.equalsIgnoreCase(REMOVE_TEMPLATES)) {
            return new AutomationCommandRemove(REMOVE_TEMPLATES, params, TEMPLATE_REGISTRY, this);
        }
        if (command.equalsIgnoreCase(REMOVE_MODULE_TYPES)) {
            return new AutomationCommandRemove(REMOVE_MODULE_TYPES, params, MODULE_TYPE_REGISTRY, this);
        }
        if (command.equalsIgnoreCase(REMOVE_RULE)) {
            return new AutomationCommandRemove(REMOVE_RULE, params, RULE_REGISTRY, this);
        }
        if (command.equalsIgnoreCase(REMOVE_RULES)) {
            return new AutomationCommandRemove(REMOVE_RULES, params, RULE_REGISTRY, this);
        }
        if (command.equalsIgnoreCase(ENABLE_RULE)) {
            return new AutomationCommandEnableRule(ENABLE_RULE, params, RULE_REGISTRY, this);
        }
        return null;
    }

    /**
     * Build a command usage string.
     *
     * You should always use that function to use a usage string that complies
     * to a standard format.
     *
     * @param description
     *            the description of the command
     * @return a usage string that complies to a standard format
     */
    protected String buildCommandUsage(final String description) {
        return String.format("%s - %s", getCommand(), description);
    }

    /**
     * Build a command usage string.
     *
     * You should always use that function to use a usage string that complies
     * to a standard format.
     *
     * @param syntax
     *            the syntax format
     * @param description
     *            the description of the command
     * @return a usage string that complies to a standard format
     */
    protected String buildCommandUsage(final String syntax, final String description) {
        return String.format("%s %s - %s", getCommand(), syntax, description);
    }

    @Override
    public Collection<Rule> getRules() {
        if (ruleRegistry != null) {
            return ruleRegistry.getAll();
        } else {
            return null;
        }
    }

    @Override
    public RuleStatus getRuleStatus(String ruleUID) {
        if (ruleRegistry != null) {
            RuleStatusInfo rsi = ruleRegistry.getStatusInfo(ruleUID);
            return rsi != null ? rsi.getStatus() : null;
        } else {
            return null;
        }
    }

    @Override
    public void setEnabled(String uid, boolean isEnabled) {
        if (ruleRegistry != null) {
            ruleRegistry.setEnabled(uid, isEnabled);
        }

    }

}
