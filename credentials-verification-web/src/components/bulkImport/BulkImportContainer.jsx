import React, { useState, useRef } from 'react';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { withApi } from '../providers/withApi';
import BulkImport from './BulkImport';
import { withRedirector } from '../providers/withRedirector';
import Logger from '../../helpers/Logger';
import { excelToFileReader } from '../../helpers/fileHelpers';

const BulkImportContainer = ({ api, redirector: { redirectToConnections } }) => {
  const { t } = useTranslation();

  const [currentStep, setCurrentStep] = useState(0);
  const [excelFile, setExcelFile] = useState();

  const uploadRef = useRef(null);

  const saveBulk = () =>
    api
      .importBulk(api.isIssuer(), excelFile)
      .then(redirectToConnections)
      .catch(error => {
        Logger.error('Error while uploading spreadsheet', error);
        message.error(t('errors.spreadsheetUpload'));
      });

  const uploadBulkExcel = spreadSheet => excelToFileReader(spreadSheet).then(setExcelFile);

  const downloadFile = () => {
    setCurrentStep(currentStep + 1);
  };

  return (
    <BulkImport
      currentStep={currentStep}
      downloadFile={downloadFile}
      uploadRef={uploadRef}
      uploadBulkExcel={uploadBulkExcel}
      setCurrentStep={setCurrentStep}
      showNext={!!excelFile}
      next={saveBulk}
    />
  );
};

BulkImportContainer.propTypes = {
  api: PropTypes.shape({
    isIssuer: PropTypes.func,
    importBulk: PropTypes.func
  }).isRequired,
  redirector: PropTypes.shape({ redirectToConnections: PropTypes.func }).isRequired
};

export default withApi(withRedirector(BulkImportContainer));
