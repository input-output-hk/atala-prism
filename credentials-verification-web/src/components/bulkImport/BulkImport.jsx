import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import DownloadStep from './Molecules/SpreadSheetStep/DownloadStep';
import UploadStep from './Molecules/SpreadSheetStep/UploadStep';

import './_style.scss';
import { refShape } from '../../helpers/propShapes';
import CustomButton from '../common/Atoms/CustomButton/CustomButton';

const BulkImport = ({
  currentStep,
  uploadRef,
  uploadBulkExcel,
  setCurrentStep,
  showNext,
  next
}) => {
  const { t } = useTranslation();

  return (
    <div className="BulkImportContainer Wrapper">
      <div className="ContentHeader">
        <h1>{t('bulkImport.title')}</h1>
      </div>
      <div className="BulkImportContent">
        <DownloadStep currentStep={currentStep} setAsCurrentStep={setCurrentStep} />
        <UploadStep
          currentStep={currentStep}
          uploadBulkExcel={uploadBulkExcel}
          setAsCurrentStep={setCurrentStep}
          uploadRef={uploadRef}
        />
      </div>
      <div className="FooterButton">
        {showNext && (
          <CustomButton
            buttonProps={{ onClick: next, className: 'theme-outline' }}
            buttonText={t('actions.next')}
          />
        )}
      </div>
    </div>
  );
};

BulkImport.propTypes = {
  currentStep: PropTypes.number.isRequired,
  uploadRef: refShape.isRequired,
  uploadBulkExcel: PropTypes.func.isRequired,
  setCurrentStep: PropTypes.func.isRequired,
  showNext: PropTypes.bool.isRequired,
  next: PropTypes.func.isRequired
};

export default BulkImport;
