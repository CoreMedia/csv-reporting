import Upload_properties from "@coremedia/studio-client.cap-base-models/upload/Upload_properties";
import RemoteService from "@coremedia/studio-client.client-core-impl/data/impl/RemoteService";
import ValueExpression from "@coremedia/studio-client.client-core/data/ValueExpression";
import ValueExpressionFactory from "@coremedia/studio-client.client-core/data/ValueExpressionFactory";
import EventUtil from "@coremedia/studio-client.client-core/util/EventUtil";
import FileWrapper from "@coremedia/studio-client.main.editor-components/sdk/upload/FileWrapper";
import FileContainer from "@coremedia/studio-client.main.editor-components/sdk/upload/dialog/FileContainer";
import FileContainersObservable from "@coremedia/studio-client.main.editor-components/sdk/upload/dialog/FileContainersObservable";
import UploadDialog from "@coremedia/studio-client.main.editor-components/sdk/upload/dialog/UploadDialog";
import MessageBoxUtil from "@coremedia/studio-client.main.editor-components/sdk/util/MessageBoxUtil";
import Ext from "@jangaroo/ext-ts";
import Container from "@jangaroo/ext-ts/container/Container";
import MessageBoxWindow from "@jangaroo/ext-ts/window/MessageBox";
import { as, bind } from "@jangaroo/runtime";
import Config from "@jangaroo/runtime/Config";
import int from "@jangaroo/runtime/int";
import ConfigUtils from "@jangaroo/runtime/ConfigUtils";
import UploadSettingsService from "@coremedia/studio-client.main.editor-components/sdk/upload/UploadSettingsService";

interface CSVImportDialogBaseConfig extends Config<UploadDialog> {
}

class CSVImportDialogBase extends UploadDialog {
  declare Config: CSVImportDialogBaseConfig;

  #fileContainers: FileContainersObservable = null;

  #validationExpression: ValueExpression = null;

  #uploadDropAreaDisabled: boolean = false;

  constructor(config: Config<CSVImportDialogBase> = null) {
    super(ConfigUtils.apply(Config(CSVImportDialogBase, {
      settings: UploadSettingsService.getInstance().getUploadSettings()
    }), config));
  }

  /**
   * Returns the value expression that enables/disables the upload button.
   * the status of the buttons depends on if all file panels on this dialog are valid.
   * @return
   */
  protected override getUploadButtonDisabledExpression(): ValueExpression {
    if (!this.#validationExpression) {
      this.#validationExpression = ValueExpressionFactory.createFromFunction((): boolean => {
        if (!this.#fileContainers) {
          this.#fileContainers = new FileContainersObservable();
          this.#fileContainers.getInvalidityExpression().setValue(true);
        }

        if (this.#fileContainers.getInvalidityExpression().getValue()) {
          return true;
        }

        // Check that the file is a CSV
        if (this.#fileContainers.getFiles().length != 1) {
          return true;
        }
        const fileType: string = this.#fileContainers.getFiles()[0].get(FileWrapper.FILE_TYPE_PROPERTY);
        if (fileType != "csv") {
          return true;
        }
      });
    }
    return this.#validationExpression;
  }

  /**
   * Fired when a file object has been dropped on the target drop area.
   * The file drop plugin fire an event for each file that is dropped
   * and the corresponding action is handled here.
   */
  protected override handleDrop(files: Array<any>): void {
    if (!this.#uploadDropAreaDisabled) {
      MessageBoxWindow.getInstance().show({
        title: Upload_properties.Upload_progress_title,
        msg: Upload_properties.Upload_progress_msg,
        closable: false,
        width: 300,
      });
      EventUtil.invokeLater((): void => {//otherwise the progress bar does not appear :(
        for (let i = 0; i < files.length; i++) {
          const fc = Config(FileContainer);
          fc.file = files[i];
          fc.settings = this.settings;
          fc.removeFileHandler = bind(this, this.removeFileContainer);
          const fileContainer = new FileContainer(fc);
          this.#fileContainers.add(fileContainer);
          this.#uploadDropAreaDisabled = true;
        }
        MessageBoxWindow.getInstance().hide();
        this.#refreshPanel();
      });
    }
  }

  /**
   * Removes the given file container from the list of uploading files.
   * @param fileContainer
   */
  override removeFileContainer(fileContainer: FileContainer): void {
    this.#fileContainers.remove(fileContainer);
    if (this.#fileContainers.isEmpty()) {
      this.#uploadDropAreaDisabled = false;
    }
    this.#refreshPanel();
  }

  /**
   * Rebuilds all panels representing a future upload.
   */
  #refreshPanel(): void {
    const dropArea = as(Ext.getCmp(UploadDialog.DROP_BOX), Container);
    if (this.#uploadDropAreaDisabled) {
      dropArea.hide();
    } else {
      dropArea.show();
    }

    //clear and add list of upload containers
    const list = as(Ext.getCmp(UploadDialog.UPLOAD_LIST), Container);
    let fileContainer: FileContainer = null;
    for (let i = 0; i < this.#fileContainers.size(); i++) {
      fileContainer = this.#fileContainers.getAt(i);
      if (!fileContainer.rendered) {
        list.add(fileContainer);
      }
    }
  }

  protected override okPressed(): void {
    const fileWrappers = this.#fileContainers.getFiles();
    const url = RemoteService.calculateRequestURI("importcsv/uploadfile");
    fileWrappers.forEach((fileWrapper: FileWrapper): void =>
    //fileWrapper.setCustomUploadUrl('importcsv/uploadfile');
    //fileWrapper.upload(settings, null, onSuccess, uploadError, progress);

      //upload(url, settings, null, onSuccess, uploadError, progress);
      this.html5upload(url, fileWrapper.getFile()),
    );

    this.close();
    //UploadManager.bulkUpload(settings, null, fileWrappers, callback);
  }

  #onSuccess(): void {
    MessageBoxUtil.showInfo("Import Status", "Successfully updated content");
  }

  protected uploadError(response: XMLHttpRequest): void {
    const result = RemoteService.createRemoteError(response.responseText, "POST", response.status,
      response.statusText);
    const message = result.message;
    MessageBoxUtil.showError("Import Status", "Import failed: " + message);
  }

  html5upload(url: string, file: any, headerParameters?: any, contentName?: string): void {
    const formData: any = new window["FormData"](); // TODO: add FormData class to Jangaroo libs

    const fileName: string = file.name || file.fileName; // safari and chrome use the non std. fileX props

    if (fileName) {
      formData.append("file", file, fileName);
    } else {
      formData.append("file", file);
    }

    if (contentName) {
      formData.append("contentName", contentName);
    }

    const uploadRequest: XMLHttpRequest = new XMLHttpRequest();

    uploadRequest.open("POST", url, true);

    uploadRequest["onload"] = ((e: any): void => this.#uploadCallback(uploadRequest));

    // TODO[rre]: if we could use Ajax.request here instead of XMLHttpRequest, the header would come for free
    uploadRequest.setRequestHeader(RemoteService.getCsrfTokenHeaderName(), RemoteService.getCsrfTokenValue());

    if (headerParameters) {
      for (const key in headerParameters) {
        uploadRequest.setRequestHeader(key, headerParameters[key]);
      }
    }

    uploadRequest.send(formData);
  }

  #uploadCallback(response: XMLHttpRequest): void {
    const status: int = response.status;
    if (status == 204 || status == 200 || status == 201) {
      this.#onSuccess();
    } else {
      this.uploadError(response);
    }
  }
}

export default CSVImportDialogBase;
