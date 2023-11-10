# CoreMedia CSV Reporting Extension

The CoreMedia CSV Reporting Extension allows users to generate reports in CSV format describing the state of a bulk selection of content. Content may be selected in the same manner as a content search is performed in studio. This is implemented as a blueprint extension titled “CSV”, which includes modules for the studio and preview CAE components.
## About this Project
Maintained by Kevin Cherniawski, Alia Robinson, and Lihan Zhang

### Dependencies
This extension introduces a dependency on the third-party library “superCSV”, which is licensed under the Apache License version 2. 

### Versioning
Versions of the CSV Reporter extension correspond to CoreMedia releases. For example, version 1904.1-1 of the CSV reporter is compatible with CoreMedia version 1904.1.

# Installation
1. Add the CSV extension to the workspace as a submodule:
    ```
    git submodule add https://github.com/CoreMedia/csv-reporting.git modules/extensions/csv
    ```
2. Run the CoreMedia Extension Tool to add the csv extension module dependencies:
    ```
    cd workspace-configuration/extensions
    mvn extensions:sync -Denable=csv
    ```
3. Add the `csv-common`, `csv-importer` and `csv-cmd` modules to the Maven project build, e.g. as sub-modules of modules/extensions: 
   ```
   <modules>
   ...
     <module>csv/csv-common</module>
     <module>csv/csv-importer</module>
     <module>csv/csv-cmd</module>
   </modules>
   ```
   You'll also need to make sure modules/extensions is added as a module to the root pom.xml.
4. Rebuild the workspace

## URL Configuration
The URL of the preview CAE must be configured in the properties file for the studio webapp. Assuming that the studio and preview CAE are deployed on the same machine, this can be done by adding the following line to the file
`modules\studio\studio-webapp\src\main\webapp\WEB-INF\application.properties` in the blueprint workspace:
```
studio.previewRestUrlPrefix=http://localhost:40980/blueprint/servlet
```

## URL Configuration (Docker)
If this is being deployed using docker, the URL of the preview CAE must be configured in the properties file of the studio rest service. Add the following line to the file `studio-rest-service\src\docker\config\application.properties` in the docker deployment workspace:
```
studio.previewRestUrlPrefix=https://preview${hostname.delimiter}${environment.fqdn}/blueprint/servlet
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
* [Tutorial](https://github.com/CoreMedia/csv-reporting/blob/master/documentation/Tutorial.md)
* [Configuration](https://github.com/CoreMedia/csv-reporting/blob/master/documentation/Configuration.md)
* [Automatic Reporting](https://github.com/CoreMedia/csv-reporting/blob/master/documentation/AutomaticReporting.md)
* [API](https://github.com/CoreMedia/csv-reporting/blob/master/documentation/API.md)
