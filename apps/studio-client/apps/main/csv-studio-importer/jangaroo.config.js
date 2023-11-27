const { jangarooConfig } = require("@jangaroo/core");

module.exports = jangarooConfig({
  type: "code",
  sencha: {
    name: "com.coremedia.blueprint__csv-studio-importer",
    namespace: "com.coremedia.csv.studio.importer",
    studioPlugins: [
      {
        mainClass: "com.coremedia.csv.studio.importer.CSVImportStudioPlugin",
        name: "CSVImportStudioPlugin",
      },
    ],
  },
  command: {
    build: {
      ignoreTypeErrors: true,
    },
  },
  appManifests: {
    en: {
      categories: [
        "Content",
      ],
      cmServiceShortcuts: [
        {
          cmKey: "cmCSVImport",
          cmOrder: 100,
          cmCategory: "Content",
          name: "CSV Import",
          url: "",
          cmAdministrative: false,
          cmGroups: ["csv-importer", "csv-importer@cognito"],
          cmService: {
            name: "launchSubAppService",
            method: "launchSubApp",
          },
        },
      ],
    },
  },
});
