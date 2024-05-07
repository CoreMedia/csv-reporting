# Tutorial

In the Library, a new toolbar option has been added: “CSV”. 

### Browse Mode
In Browse mode, clicking the CSV button will initiate an export of all content in the currently selected folder, including content in subfolders. 

![](https://github.com/CoreMedia/csv-reporting/blob/master/documentation/img/FolderExport1.png)

When the button is pressed, a pop-up dialog indicates the selected folder, which contains the content to export. A different template may be selected for the report by using the dropdown list. The export buttons are disabled if there are no templates available. 

There are two options for generating the CSV export:
* Pressing the “Direct Export” button will initiate the *synchronous* generation of a new CSV export, which is automatically downloaded by the browser. 
* Pressing the "Background Export" button will initiate the *asynchronous* generation of a new CSV export using a Studio Job.
  * Please open the Jobs Window in the top right corner to see the Jobs.
  * The generated CSV export will be stored as a Download in the Studio user's home folder. 

![](https://github.com/CoreMedia/csv-reporting/blob/master/documentation/img/FolderExport2.png)

### Search Mode
In Search mode, clicking the CSV button will initiate an export of all content matching the current search parameters.  In the following example, the content is filtered by several facets: folder, type, status, and last edited by. 23 items are found by the studio search.

![](https://github.com/CoreMedia/csv-reporting/blob/master/documentation/img/SearchExport1.png)

When the button is pressed, a pop-up dialog indicating a search results export is shown. A different template may be selected for the report by using the dropdown list. The export buttons are disabled if there are no templates available.  

The same export options outlined above for Browse mode are also available in Search mode. 
![](https://github.com/CoreMedia/csv-reporting/blob/master/documentation/img/SearchExport2.png)
