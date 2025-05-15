package com.trianguloy.urlchecker.modules;

import com.trianguloy.urlchecker.activities.ModulesActivity;
import com.trianguloy.urlchecker.dialogs.MainDialog;

import java.util.Collections;
import java.util.List;

public abstract class AModuleData {

    /** @return the unique identifier of this module */
    public abstract String getId();

    /** @return the user visible name of this module */
    public abstract int getName();

    /** @return whether this module should be enabled by default */
    public boolean isEnabledByDefault() {
        return true;
    }

    /**
     * Returns the dialog fragment of this module
     *
     * @param cntx for the fragment
     * @return the initialized module dialog class
     */
    public abstract AModuleDialog getDialog(MainDialog cntx);

    /**
     * Returns the configuration fragment of this module
     *
     * @param cntx for the fragment
     * @return the initialized module configuration class
     */
    public abstract AModuleConfig getConfig(ModulesActivity cntx);

    /** Returns the list of automations this module can provide */
    public List<AutomationRules.Automation<AModuleDialog>> getAutomations() {
        return Collections.emptyList();
        // implementation notes: due to how java manages generics, you may need to add an unchecked cast
        // return (List<AutomationRules.Automation<AModuleDialog>>) (List<?>) AUTOMATIONS;
    }
}
