# API

## Studio
The CSV Export endpoint accepts a set of parameters for searching and filtering content. All content matching the search criteria is included in the response in CSV format.

The parameters for this endpoint mirror those of the Search API in studio. See the `SearchParameters` documentation for more information on the usage of these parameters. Additionally, the required “template” parameter specifies the name of the template used to generate the report.

* Request URL: /exportcsv/contentset
* Method: GET
* Response Content Type: text/csv
* Request Parameters: 
  * query
  * limit
  * sortCriteria
  * folderUri
  * includeSubFolders
  * contentTypeNames
  * includeSubTypes
  * filterQueries
  * facetFieldCriteria
  * facetQueries
  * searchHandler
  * template

Alternatively, the new background CSV export requires using endpoints of the Studio Job Framework and the following steps/calls:

1. Start of the CSV Export Job with POST to /rest/api/jobService/csvReporterExport with job payload (see required parameters above)
2. Polling of the Job Status with GET to /rest/api/job/[JOB-ID] (JOB-ID returned by previous call)
3. Once status is SUCCESS, retrieve blob from CMDownload blob with GET to /rest/api/content/[CONTENT-ID]/properties/data (returned in last call)

## CAE
The Content Set Export endpoint accepts a set of content IDs and outputs a CSV with metadata for the requested content items. This endpoint is available ONLY on the preview CAE

* Request URL: /contentsetexport/{template}
* Method: POST
* Request Content Type: application/json
* Response Content Type: text/csv
* Request Parameters:
  * template: The name of the template used to generate the report
* Request Body: A JSON array of numeric content IDs. 

Example: `[1111,1112,1113,1114]`

An additional endpoint exists under the request URL /contentsetexportnoheader/{template} which will omit the header row in the response.  
