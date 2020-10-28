import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Steps } from 'antd';
import CustomButton from '../../Atoms/CustomButton/CustomButton';
import './_style.scss';

const createSteps = count => {
  if (count <= 1) return [];

  const steps = [];
  for (let i = 0; i < count; i++) steps.push(<Steps.Step key={i} />);

  return steps;
};

const StepFooter = ({
  currentStep,
  stepCount,
  previousStep,
  nextStep,
  renderExtraOptions,
  finishText,
  disablePrevious,
  disableNext,
  onCancel,
  onFinish
}) => {
  const { t } = useTranslation();

  const firstStep = currentStep === 0;
  const lastStep = currentStep + 1 === stepCount;

  return (
    <div className="StepsFooter">
      <div className="BackButtons">
        <CustomButton
          buttonProps={{
            onClick: firstStep ? onCancel : previousStep,
            className: 'theme-grey',
            disabled: disablePrevious
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
          buttonProps={{
            onClick: lastStep ? onFinish : nextStep,
            className: 'theme-primary',
            disabled: disableNext
          }}
          buttonText={t(lastStep ? finishText : 'actions.next')}
        />
      </div>
    </div>
  );
};

StepFooter.defaultProps = {
  renderExtraOptions: () => {},
  finishText: 'actions.next',
  disablePrevious: false,
  disableNext: false
};

StepFooter.propTypes = {
  currentStep: PropTypes.number.isRequired,
  stepCount: PropTypes.number.isRequired,
  previousStep: PropTypes.func.isRequired,
  nextStep: PropTypes.func.isRequired,
  renderExtraOptions: PropTypes.func,
  finishText: PropTypes.string,
  disablePrevious: PropTypes.bool,
  disableNext: PropTypes.bool,
  onCancel: PropTypes.func.isRequired,
  onFinish: PropTypes.func.isRequired
};

export default StepFooter;
