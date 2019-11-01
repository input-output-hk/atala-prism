import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Col, Steps, Row } from 'antd';
import CustomButton from '../../Atoms/CustomButton/CustomButton';

import './_style.scss';

const createSteps = count => {
  const steps = [];
  for (let i = 0; i < count; i++) steps.push(<Steps.Step key={i} />);

  return steps;
};

const StepFooter = ({
  currentStep,
  stepCount,
  previousStep,
  nextStep,
  finish,
  renderExtraOptions,
  finishText
}) => {
  const { t } = useTranslation();

  const lastStep = currentStep + 1 === stepCount;

  return (
    <div className="StepsFooter">
      <div className="BackButtons">
        <CustomButton
          buttonProps={{
            onClick: previousStep,
            className: 'theme-grey',
            disabled: currentStep === 0
          }}
          buttonText={t('actions.back')}
        />
      </div>
      <div className="StepContainer">
        <Steps current={currentStep}>{createSteps(stepCount)}</Steps>
      </div>
      <div className="ContinueButtons">
        {renderExtraOptions && <div className="SaveButton">{renderExtraOptions()}</div>}
        <CustomButton
          buttonProps={{ onClick: lastStep ? finish : nextStep, className: 'theme-primary' }}
          buttonText={t(lastStep ? finishText : 'actions.next')}
        />
      </div>
    </div>
  );
};

StepFooter.defaultProps = {
  renderExtraOptions: null,
  previousStep: null,
  nextStep: null
};

StepFooter.propTypes = {
  currentStep: PropTypes.number.isRequired,
  stepCount: PropTypes.number.isRequired,
  previousStep: PropTypes.func,
  nextStep: PropTypes.func,
  finish: PropTypes.func.isRequired,
  renderExtraOptions: PropTypes.func,
  finishText: PropTypes.string.isRequired
};

export default StepFooter;
