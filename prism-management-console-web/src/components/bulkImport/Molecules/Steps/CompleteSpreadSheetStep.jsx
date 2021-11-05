import React, { useState } from 'react';
import PropTypes from 'prop-types';
import Papa from 'papaparse';
import chardet from 'jschardet';
import { useTranslation } from 'react-i18next';
import { DownloadOutlined, UploadOutlined } from '@ant-design/icons';
import { Upload, message, Button } from 'antd';
import GenericStep from './GenericStep';
import { downloadTemplateCsv } from '../../../../helpers/fileHelpers';
import { contactShape, credentialTypeShape } from '../../../../helpers/propShapes';
import {
  COMPLETE_SPREADSHEET_STEP,
  ENCODING_UTF,
  ENCODING_ISO
} from '../../../../helpers/constants';
import './_style.scss';

const CompleteSpreadSheetStep = ({
  currentStep,
  setCurrentStep,
  recipients,
  credentialType,
  setFileData,
  showStepNumber,
  headersMapping,
  isEmbedded
}) => {
  const { t } = useTranslation();

  const [selectedFileList, setSelectedFileList] = useState();

  // fileList's length is checked to avoid reading a removed file
  const handleChange = ({ file, fileList }) => setSelectedFileList(fileList.length ? [file] : null);

  const handleRemoveFile = () => setSelectedFileList(null);

  function handleFileRequest(params) {
    const reader = new FileReader();
    const blob = new Blob([params.file], { type: 'text/csv' });

    reader.readAsText(blob);
    reader.onload = e => {
      const detection = chardet.detect(e.target.result);
      // If jschardet determine 'pure ascii' means that it contains special ascii characters such as
      // "�", this aren't expected as a result of parsing
      // This is because with UTF-8 doesn't parse file correctly, after this retry with ISO format
      // ENCODING_ISO corresponds to default excel format (if before file saving it doesn't
      // specified UTF-8)
      // ENCODING_UTF corresponds to default web (google sheet for example) format
      const encoding = detection.encoding.includes('ascii') ? ENCODING_ISO : ENCODING_UTF;
      parseFile({ ...params, encoding });
    };
  }

  const parseFile = ({ onSuccess, onError, file, encoding = '' }) => {
    Papa.parse(file, {
      encoding,
      skipEmptyLines: true,
      complete: result => {
        setFileData({
          ...result,
          fileObj: file
        });
        if (result.errors.length) showFileFormatError(onError, result.errors);
        else {
          message.success(t('bulkImport.success.uploadedFile'));
          onSuccess(null, file);
        }
      },
      error: err => showFileFormatError(onError, err)
    });
  };

  const showFileFormatError = (onError, err) => {
    onError(err, t('bulkImport.error.fileFormat'));
    message.error(t('bulkImport.error.fileFormat'));
  };

  const uploaderProps = {
    accept: '.csv',
    multiple: false,
    fileList: selectedFileList,
    onRemove: handleRemoveFile,
    onChange: handleChange,
    customRequest: handleFileRequest
  };

  const props = {
    step: COMPLETE_SPREADSHEET_STEP,
    currentStep,
    title: t('bulkImport.completeSpreadsheet.title'),
    info: t('bulkImport.completeSpreadsheet.info'),
    actions: (
      <>
        <Button
          className="fileActionButton"
          onClick={() =>
            downloadTemplateCsv({ contacts: recipients, credentialType }, headersMapping)
          }
        >
          {t('bulkImport.completeSpreadsheet.downloadText')}
          <DownloadOutlined className="Icon" />
        </Button>
        <Upload {...uploaderProps}>
          <Button className="fileActionButton">
            {t('bulkImport.completeSpreadsheet.uploadText')}
            <UploadOutlined className="Icon" />
          </Button>
        </Upload>
      </>
    ),
    setCurrentStep,
    showStepNumber,
    isEmbedded
  };

  return <GenericStep {...props} />;
};

CompleteSpreadSheetStep.defaultProps = {
  inputData: null,
  setCurrentStep: undefined,
  recipients: null,
  credentialType: null,
  showStepNumber: true
};

CompleteSpreadSheetStep.propTypes = {
  recipients: PropTypes.arrayOf(PropTypes.shape(contactShape)),
  credentialType: credentialTypeShape,
  currentStep: PropTypes.number.isRequired,
  setCurrentStep: PropTypes.func,
  inputData: PropTypes.shape({
    contacts: PropTypes.arrayOf(
      PropTypes.shape({
        externalId: PropTypes.string
      })
    )
  }),
  setFileData: PropTypes.func.isRequired,
  showStepNumber: PropTypes.bool,
  headersMapping: PropTypes.arrayOf(
    PropTypes.shape({ key: PropTypes.string, translation: PropTypes.string })
  ).isRequired,
  isEmbedded: PropTypes.bool.isRequired
};

export default CompleteSpreadSheetStep;
