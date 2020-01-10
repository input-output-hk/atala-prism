import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import GenericStep from './GenericStep';
import { DOWNLOAD_STEP } from '../../../../helpers/constants';

const UploadStep = ({ currentStep, setAsCurrentStep }) => {
  const { t } = useTranslation();

  const props = {
    step: DOWNLOAD_STEP,
    currentStep,
    stepType: 'download',
    button: (
      <Link to="Download Template" target="_blank" download>
        {t('bulkImport.download.buttonText')}
      </Link>
    ),
    changeStep: setAsCurrentStep
  };

  return <GenericStep {...props} />;
};

UploadStep.propTypes = {
  currentStep: PropTypes.number.isRequired,
  setAsCurrentStep: PropTypes.func.isRequired
};

export default UploadStep;
