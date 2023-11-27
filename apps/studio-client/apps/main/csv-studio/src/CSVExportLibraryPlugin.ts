import IconButton from "@coremedia/studio-client.ext.ui-components/components/IconButton";
import AddItemsPlugin from "@coremedia/studio-client.ext.ui-components/plugins/AddItemsPlugin";
import NestedRulesPlugin from "@coremedia/studio-client.ext.ui-components/plugins/NestedRulesPlugin";
import ICollectionView from "@coremedia/studio-client.main.editor-components/sdk/collectionview/ICollectionView";
import RepositoryToolbar from "@coremedia/studio-client.main.editor-components/sdk/collectionview/RepositoryToolbar";
import SearchToolbar from "@coremedia/studio-client.main.editor-components/sdk/collectionview/SearchToolbar";
import Component from "@jangaroo/ext-ts/Component";
import { as } from "@jangaroo/runtime";
import Config from "@jangaroo/runtime/Config";
import ConfigUtils from "@jangaroo/runtime/ConfigUtils";
import CSVExportStudioPlugin_properties from "./CSVExportStudioPlugin_properties";
import OpenCSVExportByFolderDialogAction from "./OpenCSVExportByFolderDialogAction";
import OpenCSVExportContentSetDialogAction from "./OpenCSVExportContentSetDialogAction";

interface CSVExportLibraryPluginConfig extends Config<NestedRulesPlugin> {
}

class CSVExportLibraryPlugin extends NestedRulesPlugin {
  declare Config: CSVExportLibraryPluginConfig;

  #selectionHolder: ICollectionView = null;

  #__initialize__(config: Config<CSVExportLibraryPlugin>): void {
    this.#selectionHolder = as(config.cmp, ICollectionView);
  }

  constructor(config: Config<CSVExportLibraryPlugin> = null) {
    // @ts-expect-error Ext JS semantics
    const this$ = this;
    this$.#__initialize__(config);
    super(ConfigUtils.apply(Config(CSVExportLibraryPlugin, {
      rules: [

        /* Add to the toolbar visible from the library's repository browsing view */
        Config(RepositoryToolbar, {
          plugins: [
            Config(AddItemsPlugin, {
              items: [
                /* Add a CSV export icon to the toolbar */
                Config(IconButton, {
                  itemId: "folderExportCSVActionBtn",
                  text: "CSV",
                  tooltip: CSVExportStudioPlugin_properties.library_browse_export_btn_tooltip,
                  baseAction: new OpenCSVExportByFolderDialogAction({ contentValueExpression: this$.#selectionHolder.getSelectedFolderValueExpression() }),
                }),
              ],
              after: [
                Config(Component, { itemId: RepositoryToolbar.DELETE_BUTTON_ITEM_ID }),
              ],
            }),
          ],
        }),

        /* Add to the toolbar visible from the library's search view */
        Config(SearchToolbar, {
          plugins: [
            Config(AddItemsPlugin, {
              items: [
                /* Add a CSV export icon to the toolbar */
                Config(IconButton, {
                  itemId: "searchExportCSVActionBtn",
                  text: "CSV",
                  tooltip: CSVExportStudioPlugin_properties.library_search_export_btn_tooltip,
                  baseAction: new OpenCSVExportContentSetDialogAction({ searchResultsValueExpression: this$.#selectionHolder.getSearchResultValueExpression() }),
                }),
              ],
              after: [
                Config(Component, { itemId: RepositoryToolbar.DELETE_BUTTON_ITEM_ID }),
              ],
            }),
          ],
        }),

      ],
    }), config));
  }
}

export default CSVExportLibraryPlugin;
