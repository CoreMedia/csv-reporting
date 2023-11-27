const { jangarooConfig } = require("@jangaroo/core");

module.exports = jangarooConfig({
  type: "code",
  sencha: {
    name: "com.coremedia.blueprint__csv-studio",
    namespace: "com.coremedia.csv.studio",
    studioPlugins: [
      {
        requiredGroup: "csv-reporter",
        mainClass: "com.coremedia.csv.studio.CSVExportStudioPlugin",
        name: "CSV Exporter",
      },
    ],
  },
  command: {
    build: {
      ignoreTypeErrors: true,
    },
  },
});
