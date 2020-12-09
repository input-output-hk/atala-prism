import React, { useState } from 'react';
import PropTypes from 'prop-types';
import Papa from 'papaparse';
import chardet from 'jschardet';
import { useTranslation } from 'react-i18next';
import { Icon, Upload, message, Button } from 'antd';
import GenericStep from './GenericStep';
import { downloadTemplateCsv } from '../../../../helpers/fileHelpers';
import {
  COMPLETE_SPREADSHEET_STEP,
  ENCODING_UTF,
  ENCODING_ISO
} from '../../../../helpers/constants';
import './_style.scss';

const CompleteSpreadSheetStep = ({
  currentStep,
  setCurrentStep,
  getTargets,
  setFileData,
  showStepNumber,
  headersMapping,
  isEmbedded
}) => {
  const { t } = useTranslation();

  const [selectedFileList, setSelectedFileList] = useState();

  const handleDownload = async () => {
    const targets = getTargets ? await getTargets() : null;

    downloadTemplateCsv(targets, headersMapping);
  };

  // fileList's length is checked to avoid reading a removed file
  const handleChange = ({ file, fileList }) => setSelectedFileList(fileList.length ? [file] : null);

  const handleRemoveFile = () => setSelectedFileList(null);

  function handleFileRequest(params) {
    const reader = new FileReader();
    const blob = new Blob([params.file], { type: 'text/csv' });

    reader.readAsText(blob);
    reader.onload = e => {
      const detection = chardet.detect(e.target.result);
      // If jschardet determine 'pure ascii' means that it contains special ascii characters such as "�", this aren't expected as a result of parsing
      // This is because with UTF-8 doesn't parse file correctly, after this retry with ISO format
      // ENCODING_ISO corresponds to default excel format (if before file saving it doesn't specified UTF-8)
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
        <Button className="fileActionButton" onClick={handleDownload}>
          {t('bulkImport.completeSpreadsheet.downloadText')}
          <Icon className="Icon" type="download" />
        </Button>
        <Upload {...uploaderProps}>
          <Button className="fileActionButton">
            {t('bulkImport.completeSpreadsheet.uploadText')}
            <Icon className="Icon" type="upload" />
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
  getTargets: null,
  showStepNumber: true
};

CompleteSpreadSheetStep.propTypes = {
  getTargets: PropTypes.func,
  currentStep: PropTypes.number.isRequired,
  setCurrentStep: PropTypes.func,
  inputData: PropTypes.shape({
    contacts: PropTypes.arrayOf(
      PropTypes.shape({
        externalid: PropTypes.string
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
