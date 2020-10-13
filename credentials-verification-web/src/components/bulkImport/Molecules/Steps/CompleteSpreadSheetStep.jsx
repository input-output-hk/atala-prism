import React, { useState } from 'react';
import PropTypes from 'prop-types';
import Papa from 'papaparse';
import { useTranslation } from 'react-i18next';
import { Icon, Upload, message, Button } from 'antd';
import GenericStep from './GenericStep';
import { downloadTemplateCsv } from '../../../../helpers/fileHelpers';
import { COMPLETE_SPREADSHEET_STEP } from '../../../../helpers/constants';
import './_style.scss';

const CompleteSpreadSheetStep = ({ currentStep, setCurrentStep, inputData, setFileData }) => {
  const { t } = useTranslation();

  const [selectedFileList, setSelectedFileList] = useState();

  const handleDownload = () => {
    downloadTemplateCsv(inputData);
  };

  // fileList's length is checked to avoid reading a removed file
  const handleChange = ({ file, fileList }) => setSelectedFileList(fileList.length ? [file] : null);

  const handleRemoveFile = () => setSelectedFileList(null);

  const parseFile = ({ onSuccess, onError, file }) => {
    Papa.parse(file, {
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
    customRequest: parseFile
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
    setCurrentStep
  };

  return <GenericStep {...props} />;
};

CompleteSpreadSheetStep.defaultProps = {
  inputData: null,
  setCurrentStep: undefined
};

CompleteSpreadSheetStep.propTypes = {
  currentStep: PropTypes.number.isRequired,
  setCurrentStep: PropTypes.func,
  // inputData TBD
  inputData: PropTypes.shape({
    contacts: PropTypes.arrayOf(
      PropTypes.shape({
        externalId: PropTypes.string
      })
    )
  }),
  setFileData: PropTypes.func.isRequired
};

export default CompleteSpreadSheetStep;
