/*
Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
@author Matthew Lohbihler
 */
package com.serotonin.m2m2.module;

import java.util.Collections;
import java.util.Set;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.serotonin.m2m2.db.dao.RoleDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.role.Role;
import com.serotonin.m2m2.vo.role.RoleVO;

/**
 * A permission definition allows a module to define a single permission string. The enforcement of this permission is
 * the responsibility of the module itself. The core will present a text box on the system settings permissions page
 * to allow for the editing of the permission string.
 *
 * The permission string value will be stored in the system settings table using the permission type name as the key.
 *
 * @author Matthew Lohbihler
 */
abstract public class PermissionDefinition extends ModuleElementDefinition {
    /**
     * A  human readable and translatable brief description of the permission. 
     *  Descriptions are used in the system settings permission section and so should be as brief
     *  as possible.
     *
     * @return the reference key to the permission description.
     */
    abstract public TranslatableMessage getDescription();

    /**
     * An internal identifier for this type of permission. Must be unique within an MA instance, and is recommended
     * to have the form "&lt;moduleName&gt;.&lt;permissionName&gt;" so as to be unique across all modules.
     *
     * This will be used in the system settings table to store the permission's groups
     *
     * @return the permission type name.
     */
    abstract public String getPermissionTypeName();

    /**
     * Offers the implementer the option to add default roles to the permission when the module is upgraded
     * or installed.  The roles must already exist in the roles table
     * @return - Set of roles to assign to permission
     */
    protected Set<Role> getDefaultRoles(){
        return Collections.emptySet();
    }
    
    /**
     * Get the current set of Role objects for this permission
     * @return
     */
    public Set<Role> getRoles() {
        Set<Role> roles = RoleDao.getInstance().getRoles(getPermissionTypeName());
        if(roles.isEmpty()) {
            return getDefaultRoles();
        }else {
            return roles;
        }
    }

    /**
     * Get the permission with current roles filled in
     * @return
     */
    public MangoPermission getPermission() {
        Set<RoleVO> roles = RoleDao.getInstance().getRoleVOs(getPermissionTypeName());
        if(roles.isEmpty()) {
            for(Role role : getDefaultRoles()) {
                roles.add(RoleDao.getInstance().getByXid(role.getXid(), true));
            }
            return new MangoPermission(getPermissionTypeName(), roles);
        }else {
            return new MangoPermission(getPermissionTypeName(), roles);
        }
    }
}
