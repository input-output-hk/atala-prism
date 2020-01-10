import React from 'react';
import { Avatar } from 'antd';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';

import './_style.scss';

const GenericStep = ({ step, currentStep, stepType, button, changeStep }) => {
  const { t } = useTranslation();

  const isActiveStep = step === currentStep;

  return (
    <div
      className={`StepCard ${isActiveStep ? 'IsSelected' : 'NotSelected'}`}
      onClick={() => changeStep(step)}
      onKeyUp={({ key }) => {
        if (key === 'ArrowLeft' && currentStep >= step) changeStep(currentStep - 1);
        if (key === 'ArrowRight' && currentStep <= step) changeStep(currentStep + 1);
      }}
      role="button"
      tabIndex={step}
    >
      <Avatar>{isActiveStep && step + 1}</Avatar>
      <h1>{t(`bulkImport.${stepType}.title`)}</h1>
      <p>{t(`bulkImport.${stepType}.info`)}</p>
      {button}
    </div>
  );
};

GenericStep.propTypes = {
  step: PropTypes.number.isRequired,
  currentStep: PropTypes.number.isRequired,
  stepType: PropTypes.oneOf(['download', 'upload']).isRequired,
  button: PropTypes.element.isRequired,
  changeStep: PropTypes.func.isRequired
};

export default GenericStep;
