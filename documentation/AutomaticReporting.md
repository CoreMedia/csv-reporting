Automatic Report Generation
===========================

Running the Script
------------------

In the base directory of the extension, there is a bash script called `authenticatedexport.sh`. This script can be run by a CI system to export content at regular intervals.

This script takes in required and optional arguments; below are the documented options:

*As a general reminder when bash scripting, arguments must be wrapped in quotes or they may not be parsed correctly.

Required arguments:
```
-u | -- user

The username of the user who will be used to authenticate with the Studio API.

-p | -password

The password of the user who will be used to authenticate with the Studio API. If the user would not like to pass this as an explicit argument, a silent prompt will appear for it instead if a password is not specified.

-sh | --studiohost

The hostname of where the studio webapp is running at, e.g. "studio.some.where". No protocol is required (see the option --pr). The argument must be wrapped in quotes, or will not be parsed correctly.

-qu | --queryurl

The query URL that contains the parameters for the specific export to be run. To obtain this URL, see below "How to Obtain Query URL". The argument must be wrapped in quotes, or will not be parsed correctly.
```
Optional arguments:
```
-d | --directory

The directory at which the export should be written to. The default directory is the current directory of the script.

-f | --filename

The filename of which the export should have. The default filename format is "Report_YYYY-MM-DD_HH-MM-SS.csv".

-pr | --protocol

The protocol that the export should use, i.e. `http` or `https`. Defaults to `https`.
```
How to Obtain a Query URL
-------------------------

Open studio and open the Library. Specify the desired parameters for your export job using the filters available in the Library search. This example sets up a script to export all content in the "Content" folder that was modified within the last day.

![](https://github.com/CoreMedia/csv-reporting/blob/master/documentation/img/AutoJob1.png)

Open the export dialog box (do not hit “Export” or close the dialog). There will be a collapsible panel called “Query URL” that will display the Query URL of the selected search parameters. At the bottom of the dialog box, there is a “Copy Query URL” button. This can be used to directly copy the contents of “Query URL”. This value will be the input for the option `-q | -queryurl` for the command line tool.

![](https://github.com/CoreMedia/csv-reporting/blob/master/documentation/img/AutoJob2.png)

Using the Background Job functionality
------------------------------------------
The provided script uses the synchronous endpoint for the CSV export generation.
Please see the notes in the [API documentation](API.md) for the Background Job calls in case you want to automate it. 
