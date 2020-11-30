import React, { useState } from 'react';
import PropTypes from 'prop-types';
import Papa from 'papaparse';
import { useTranslation } from 'react-i18next';
import { Icon, Upload, message, Button } from 'antd';
import GenericStep from './GenericStep';
import { downloadTemplateCsv } from '../../../../helpers/fileHelpers';
import { COMPLETE_SPREADSHEET_STEP } from '../../../../helpers/constants';
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

  const parseFile = ({ onSuccess, onError, file }) => {
    Papa.parse(file, {
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
