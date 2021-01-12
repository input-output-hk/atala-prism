import React from 'react';
import PropTypes from 'prop-types';
import { Avatar } from 'antd';
import { ARROW_LEFT, ARROW_RIGHT } from '../../../../helpers/constants';
import './_style.scss';

const STEPS_CONSTANTS = { displayShift: 1, changeBy: 1 };

const GenericStep = ({
  step,
  currentStep,
  title,
  info,
  actions,
  setCurrentStep,
  disabled,
  showStepNumber,
  isEmbedded
}) => {
  const isSelected = step === currentStep;

  const className = `StepCard
  ${isSelected ? 'Selected' : 'NotSelected'}
  ${disabled ? 'DisabledCard' : ''}
  ${isEmbedded ? 'EmbeddedStepCard' : ''}`;

  const dispayStepNumber = step + STEPS_CONSTANTS.displayShift;

  return (
    <div
      className={className}
      onClick={() => !disabled && setCurrentStep(step)}
      onKeyUp={({ key }) => {
        if (key === ARROW_LEFT && currentStep >= step)
          setCurrentStep(currentStep - STEPS_CONSTANTS.changeBy);
        if (key === ARROW_RIGHT && currentStep <= step)
          setCurrentStep(currentStep + STEPS_CONSTANTS.changeBy);
      }}
      role="button"
      tabIndex={step}
    >
      {showStepNumber && <Avatar>{isSelected && dispayStepNumber}</Avatar>}
      <div className="CardText">
        <h1>{title}</h1>
        <p>{info}</p>
      </div>
      <div className={`ActionsContainer Step${dispayStepNumber}Actions`}>{actions}</div>
    </div>
  );
};

GenericStep.defaultProps = {
  title: '',
  info: '',
  actions: null,
  setCurrentStep: undefined,
  disabled: false,
  showStepNumber: true
};

GenericStep.propTypes = {
  step: PropTypes.number.isRequired,
  currentStep: PropTypes.number.isRequired,
  title: PropTypes.string,
  info: PropTypes.string,
  actions: PropTypes.element,
  setCurrentStep: PropTypes.func,
  disabled: PropTypes.bool,
  showStepNumber: PropTypes.bool,
  isEmbedded: PropTypes.bool.isRequired
};

export default GenericStep;
