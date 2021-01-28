/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.upgrade;

import java.util.HashMap;
import java.util.Map;

import com.serotonin.m2m2.db.DatabaseType;

/**
 * Upgrade to add template system
 * 
 * @author Terry Packer
 *
 */
public class Upgrade10 extends DBUpgrade {

    @Override
    public void upgrade() throws Exception {
        // Run the script.
        Map<String, String[]> scripts = new HashMap<>();
        scripts.put(DatabaseType.DERBY.name(), derbyScript);
        scripts.put(DatabaseType.MYSQL.name(), mysqlScript);
        scripts.put(DatabaseType.MSSQL.name(), mssqlScript);
        scripts.put(DatabaseType.H2.name(), h2Script);
        runScript(scripts);
    }

    @Override
    protected String getNewSchemaVersion() {
        return "11";
    }

    private final String[] mssqlScript = { //
    		"CREATE TABLE templates ( id int NOT NULL identity, xid nvarchar(50) NOT NULL, name NVARCHAR(255), templateType NVARCHAR(50), readPermission NVARCHAR(255), setPermission NVARCHAR(255), data image NOT NULL, PRIMARY KEY (id) );", //
            "ALTER TABLE templates ADD CONSTRAINT templatesUn1 UNIQUE (xid);", //
            "ALTER TABLE dataPoints ADD COLUMN templateId INT;", //
            "ALTER TABLE dataPoints ADD CONSTRAINT dataPointsFk2 FOREIGN KEY (templateId) REFERENCES templates(id);" //
    };
    private final String[] mysqlScript = { //
    		"CREATE TABLE templates ( id int NOT NULL auto_increment, xid varchar(50) NOT NULL, name VARCHAR(255), templateType VARCHAR(50), readPermission VARCHAR(255), setPermission VARCHAR(255), data longblob NOT NULL, PRIMARY KEY (id) ) engine=InnoDB;", //
            "ALTER TABLE templates ADD CONSTRAINT templatesUn1 UNIQUE (xid);", //
            "ALTER TABLE dataPoints ADD COLUMN templateId INT;", //
            "ALTER TABLE dataPoints ADD CONSTRAINT dataPointsFk2 FOREIGN KEY (templateId) REFERENCES templates(id);" //
    };
    private final String[] derbyScript = { //
    		"CREATE TABLE templates ( id int not null generated by default as identity (start with 1, increment by 1), xid varchar(50) NOT NULL, name VARCHAR(255), templateType VARCHAR(50), readPermission VARCHAR(255), setPermission VARCHAR(255), data blob NOT NULL );", //
    		"ALTER TABLE templates ADD CONSTRAINT templatesPk primary key (id);", //
            "ALTER TABLE templates ADD CONSTRAINT templatesUn1 UNIQUE (xid);", //
            "ALTER TABLE dataPoints ADD COLUMN templateId INT;", //
            "ALTER TABLE dataPoints ADD CONSTRAINT dataPointsFk2 FOREIGN KEY (templateId) REFERENCES templates(id);" //
    }; 
    private final String[] h2Script = { //
    		"CREATE TABLE templates ( id int NOT NULL auto_increment, xid varchar(50) NOT NULL, name VARCHAR(255), templateType VARCHAR(50), readPermission VARCHAR(255), setPermission VARCHAR(255), data longblob NOT NULL, PRIMARY KEY (id) );", //
            "ALTER TABLE templates ADD CONSTRAINT templatesUn1 UNIQUE (xid);", //
            "ALTER TABLE dataPoints ADD COLUMN templateId INT;", //
            "ALTER TABLE dataPoints ADD CONSTRAINT dataPointsFk2 FOREIGN KEY (templateId) REFERENCES templates(id);" //
    };

}
