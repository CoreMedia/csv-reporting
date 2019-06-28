# CoreMedia CSV Reporting Extension

he CoreMedia CSV Reporting Extension allows users to generate reports in CSV format describing the state of a bulk selection of content. Content may be selected in the same manner as a content search is performed in studio. This is implemented as a blueprint extension titled “CSV”, which includes modules for the studio and preview CAE components.
## About this Project
Maintained by Kevin Cherniawski, Alia Robinson, and Lihan Zhang

### Dependencies
This extension introduces a dependency on the third-party library “superCSV”, which is licensed under the Apache License version 2. 

### Versioning
Versions of the CSV Reporter extension correspond to CoreMedia releases. For example, version 1904.1-1 of the CSV reporter is compatible with CoreMedia version 1904.1.

# Installation
1. Add Add the CSV extension to the workspace at modules/extensions/csv
2. Add the csv module to modules/extensions/pom.xml:
    ```
    <modules>
	    ...
	    <module>csv</module>
	    ...
    </modules>
    ```
3. Add the “csv” extension to workspace-config/extensions/managed-extensions.txt
4. Run the CoreMedia Extension Tool to add the csv extension module dependencies:
    ```
    java -jar tool/extensions.jar --task synchronize --extension-config-file  workspace-configuration/extensions/extension-config.properties --task-input-file workspace-configuration/extensions/managed-extensions.txt
    ```

## URL Configuration
The URL of the preview CAE must be configured in the properties file for the studio webapp. Assuming that the studio and preview CAE are deployed on the same machine, this can be done by adding the following line to the file
`modules\studio\studio-webapp\src\main\webapp\WEB-INF\application.properties` in the blueprint workspace:
```
studio.previewRestUrlPrefix=http://localhost:40980/blueprint/servlet
```

## URL Configuration (Docker)
If this is being deployed using docker, the URL of the preview CAE must be configured in the properties file of the studio rest service. Add the following line to the file `studio-rest-service\src\docker\config\application.properties` in the docker deployment workspace:
```
studio.previewRestUrlPrefix=http://cae-preview:40980/blueprint/servlet
```

## Reporter Group
By default, access to this extension in Studio is restricted only to members of the “reporter” group. This is done to limit availability to a select number of people, preventing too many users in the system from sending resource-intensive requests to the preview CAE at one time. 

If the reporter group is not present in the system, import it via the restoreusers command-line tool. 
```
modules/cmd-tools/cms-tools-application/target/cms-tools/bin/cm restoreusers -u <username> -p <password> -url <ior url> -f modules/extensions/csv/csv-test-data/users/users
-csv-reporting.xml
```

After creating the reporter group, add the desired users as members of this group using the CoreMedia SiteManager. 

## Reporting Settings
The CSV reporter requires a global settings document to be present at Settings/Options/Settings/ReportingSettings. Use the serverimport tool to import the provided settings document.
```
modules/cmd-tools/cms-tools-application/target/cms-tools/bin/cm serverimport -u <username> -p <username> -url <ior url> modules/extensions/csv/csv-test-data/conte
nt/Settings/Options/Settings/ReportingSettings.xml
```

# Documentation
* [Tutorial]
* [Configuration]
* [Automatic Reporting]
* [API]
