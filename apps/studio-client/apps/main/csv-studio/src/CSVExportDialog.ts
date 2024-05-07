import SpacingBEMEntities from "@coremedia/studio-client.ext.ui-components/bem/SpacingBEMEntities";
import LocalComboBox from "@coremedia/studio-client.ext.ui-components/components/LocalComboBox";
import BindListPlugin from "@coremedia/studio-client.ext.ui-components/plugins/BindListPlugin";
import BindPropertyPlugin from "@coremedia/studio-client.ext.ui-components/plugins/BindPropertyPlugin";
import VerticalSpacingPlugin from "@coremedia/studio-client.ext.ui-components/plugins/VerticalSpacingPlugin";
import ButtonSkin from "@coremedia/studio-client.ext.ui-components/skins/ButtonSkin";
import WindowSkin from "@coremedia/studio-client.ext.ui-components/skins/WindowSkin";
import DataField from "@coremedia/studio-client.ext.ui-components/store/DataField";
import Editor_properties from "@coremedia/studio-client.main.editor-components/Editor_properties";
import CollapsiblePanel from "@coremedia/studio-client.main.editor-components/sdk/premular/CollapsiblePanel";
import Button from "@jangaroo/ext-ts/button/Button";
import Label from "@jangaroo/ext-ts/form/Label";
import Separator from "@jangaroo/ext-ts/menu/Separator";
import Panel from "@jangaroo/ext-ts/panel/Panel";
import { bind } from "@jangaroo/runtime";
import Config from "@jangaroo/runtime/Config";
import ConfigUtils from "@jangaroo/runtime/ConfigUtils";
import CSVExportDialogBase from "./CSVExportDialogBase";
import CSVExportStudioPlugin_properties from "./CSVExportStudioPlugin_properties";

interface CSVExportDialogConfig extends Config<CSVExportDialogBase>, Partial<Pick<CSVExportDialog,
  "templateSelectorComboBox"
>> {
}

class CSVExportDialog extends CSVExportDialogBase {
  declare Config: CSVExportDialogConfig;

  static override readonly xtype: string = "com.coremedia.cms.editor.sdk.config.csvExportContentSetDialog";

  static readonly EXPORT_DIALOG_LABEL_ID: string = "exportContentSetLbl";

  static readonly REQUEST_URL_PANEL_ID: string = "requestURLCPnl";

  static readonly REQUEST_URL_LABEL_ID: string = "requestURLLbl";

  constructor(config: Config<CSVExportDialog> = null) {
    super(((): Config<CSVExportDialogBase> => ConfigUtils.apply(Config(CSVExportDialog, {
      title: CSVExportStudioPlugin_properties.exportDialog_title,
      id: "exportCSVDialog",
      resizable: true,
      resizeHandles: "s",
      width: 500,
      minHeight: 160,
      constrainHeader: true,
      ui: WindowSkin.GRID_200.getSkin(),
      modal: true,

      items: [
        Config(Panel, {
          itemId: "editorContainer",
          scrollable: "y",
          items: [
            Config(Label, {
              itemId: CSVExportDialog.EXPORT_DIALOG_LABEL_ID,
              text: config.confirmationMessage,
            }),
            this.templateSelectorComboBox = new LocalComboBox({
              fieldLabel: CSVExportStudioPlugin_properties.exportDialog_templateSelector_label,
              encodeItems: true,
              width: 310,
              allowBlank: false,
              displayField: "name",
              ...ConfigUtils.append({
                plugins: [
                  Config(BindListPlugin, {
                    bindTo: this.getTemplatesExpression(),
                    fields: [
                      Config(DataField, {
                        name: "name",
                        encode: false,
                      }),
                    ],
                  }),
                  Config(BindPropertyPlugin, {
                    componentEvent: "change",
                    componentProperty: "value",
                    bidirectional: true,
                    bindTo: this.getSelectedTemplateValueExpression(),
                  }),
                ],
              }),
            }),
          ],
          plugins: [
            Config(VerticalSpacingPlugin, { modifier: SpacingBEMEntities.VERTICAL_SPACING_MODIFIER_200 }),
          ],
        }),
        Config(Separator),
        Config(CollapsiblePanel, {
          itemId: CSVExportDialog.REQUEST_URL_PANEL_ID,
          title: CSVExportStudioPlugin_properties.requestURLDialog_title,
          collapsed: true,
          items: [
            Config(Label, {
              itemId: CSVExportDialog.REQUEST_URL_LABEL_ID,
              style: "word-wrap: break-word",
              plugins: [
                Config(BindPropertyPlugin, {
                  componentProperty: "text",
                  bindTo: this.getRequestURIExpression(),
                  ifUndefined: "",
                }),

              ],
            }),
          ],
        }),
      ],
      buttons: [
        Config(Button, {
          itemId: "backgroundExportBtn",
          ui: ButtonSkin.FOOTER_PRIMARY.getSkin(),
          scale: "small",
          text: CSVExportStudioPlugin_properties.exportDialog_backgroundExportButton_text,
          handler: bind(this, this.handleBackgroundExport),
          plugins: [
            Config(BindPropertyPlugin, {
              componentProperty: "disabled",
              bindTo: this.getDisabledValueExpression(),
            }),
          ],
        }),
        Config(Button, {
          itemId: "directExportBtn",
          ui: ButtonSkin.FOOTER_PRIMARY.getSkin(),
          scale: "small",
          text: CSVExportStudioPlugin_properties.exportDialog_directExportButton_text,
          handler: bind(this, this.handleDirectExport),
          plugins: [
            Config(BindPropertyPlugin, {
              componentProperty: "disabled",
              bindTo: this.getDisabledValueExpression(),
            }),
          ],
        }),
        Config(Button, {
          itemId: "copyURLToClipboardBtn",
          ui: ButtonSkin.FOOTER_SECONDARY.getSkin(),
          scale: "small",
          text: CSVExportStudioPlugin_properties.copyRequestURL_text,
          handler: bind(this, this.copyQueryURLToClipboard),
          plugins: [
            Config(BindPropertyPlugin, {
              componentProperty: "disabled",
              bindTo: this.getDisabledValueExpression(),
            }),
          ],
        }),
        Config(Button, {
          itemId: "cancelBtn",
          ui: ButtonSkin.FOOTER_SECONDARY.getSkin(),
          scale: "small",
          text: Editor_properties.dialog_defaultCancelButton_text,
          handler: bind(this, this.close),
        }),
      ],
      plugins: [
        Config(VerticalSpacingPlugin, { modifier: SpacingBEMEntities.VERTICAL_SPACING_MODIFIER_200 }),
      ],

    }), config))());
  }

  #templateSelectorComboBox: LocalComboBox = null;

  get templateSelectorComboBox(): LocalComboBox {
    return this.#templateSelectorComboBox;
  }

  set templateSelectorComboBox(value: LocalComboBox) {
    this.#templateSelectorComboBox = value;
  }
}

export default CSVExportDialog;
