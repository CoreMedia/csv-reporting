# CoreMedia CSV Reporting Extension

The CoreMedia CSV Reporting Extension allows users to generate reports in CSV format describing the state of a bulk selection of content. Content may be selected in the same manner as a content search is performed in studio. This is implemented as a blueprint extension titled “CSV”, which includes modules for the studio and preview CAE components.

## About this Project
Contributors
- Kevin Cherniawski
- Alia Robinson
- Lihan Zhang
- Henning Saul

### Dependencies
This extension introduces a dependency on the third-party library “superCSV”, which is licensed under the Apache License version 2. 

### Versioning
Versions of the CSV Reporter extension correspond to CoreMedia releases. For example, version 2107.8-1 of the CSV reporter is compatible and has been tested with CoreMedia version 2107.8.

# Installation
1. Add the CSV extension to the workspace.
   1. Option A: Git Submodule

      Add this repo or your fork (recommended) as a Git Submodule to your existing CoreMedia Blueprint-Workspace in the extensions folder. This way, you will be able to merge new commits made in this repo back to your fork.
      ```
      git submodule add https://github.com/CoreMedia/csv-reporting.git modules/extensions/csv
      ```
      Afterwards, change to modules/extensions/csv and check out the desired branch or tag.
   2. Option B: Copy files to your workspace
   
      Download the repo and copy the files into your Blueprint workspace's extension folder. 
      This way you won't be able to merge new commits made in this repo back to yours. But if you do not like Git Submodules, you don't have to deal with them.
2. Run the CoreMedia Extension Tool to add the csv extension module dependencies:
    ```
    cd workspace-configuration/extensions
    mvn extensions:sync -Denable=csv
    ```
3. Rebuild the workspace and studio-client

## URL Configuration
The studio-server needs to be configured with a URL where to reach the Preview CAE:
```
csv.previewRestUrlPrefix=http://[cae-preview-host]:[cae-preview-port]/blueprint/servlet
```

The default is as follows, which works for a standard Docker deployment:
```
http://cae-preview:8080/blueprint/servlet
```

For local development with a local studio-server and cae-preview, you will want to configure it as follows:
```
csv.previewRestUrlPrefix=http://localhost:40980/blueprint/servlet
```

## csv-reporter Group
By default, access to this extension in Studio is restricted only to members of the “csv-reporter” group. This is done to limit availability to a select number of people, preventing too many users in the system from sending resource-intensive requests to the Preview CAE at one time. 

If the reporter group is not present in the system, import it via the restoreusers command-line tool. 
```
modules/cmd-tools/cms-tools-application/target/cms-tools/bin/cm restoreusers -u <username> -p <password> -url <ior url> -f modules/extensions/csv/csv-test-data/users/users
-csv-reporting.xml
```

After creating the csv-reporter group, add the desired users as members of this group using the CoreMedia User Manager. 

For CMCC-Service customers, the above applies to the Sandbox only and group creation and user assignment can be performed using the Cloud Manager.  

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
