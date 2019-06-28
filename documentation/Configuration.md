# Configuration
CSV templates can be configured through the global settings document available at Settings/Options/Settings/ReportingSettings. The templates defined in this file determine which columns will be included the generated CSV files.
## Adding a Template
To add a new template, create a new struct as a child of “templates”, and give it a unique name. This struct must have the following two properties:
1. csvHeaders (String List)
2. csvProperties (Struct)

See the “default” template for an example of how to format custom templates. 
## Adding a Column
To add a column to the CSV exporter output, simply add the desired header to the “csvHeaders” string list in the template. Additionally, the value in this new column will need to be configured. 
### Configuring Static Properties (Metadata)
Metadata for a content object must be added to a CSV record via the Java code. Each supported metadata value corresponds to a preset header value, such as "Id", "Name", or "URL." See the “populateContentMetadataFields” method in BaseCSVUtil.java for examples of how to include such properties.
### Configuring Dynamic Properties
Content properties may be configured dynamically through the “csvProperties” struct in a template, which maps template headers to content properties. To add a new column, create an entry with the header as the key and the property name as the value. Properties defined in a content’s local settings may be used by adding the prefix “localSettings.” (including the period) before the property path. If a property does not exist for a given piece of content, the cell for that row in the spreadsheet will be left blank.  
### Configuring Custom Values
Sometimes, it may be necessary to include information in a CSV that requires custom logic to derive. Similar to the metadata approach, this will need to be included via Java code. Invoke the following from within populateCustomPropertyFields() to add the custom value to the record:
csvRecord.put(\<header>, \<custom value>); 
