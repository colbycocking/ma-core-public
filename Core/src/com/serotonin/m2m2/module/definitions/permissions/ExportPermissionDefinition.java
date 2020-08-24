/*
 * Copyright (C) 2020 Infinite Automation Systems Inc. All rights reserved.
 */

package com.serotonin.m2m2.module.definitions.permissions;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.PermissionDefinition;

public class ExportPermissionDefinition extends PermissionDefinition {
    @Override
    public TranslatableMessage getDescription() {
        return new TranslatableMessage("permission.config.export");
    }

    @Override
    public String getPermissionTypeName() {
        return "config.export";
    }
}
