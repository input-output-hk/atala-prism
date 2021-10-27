import React from 'react';
import PropTypes from 'prop-types';
import { LeftOutlined, RightOutlined } from '@ant-design/icons';
import CustomButton from '../../Atoms/CustomButton/CustomButton';
import './_style.scss';

const StepFooter = ({
  currentStep,
  stepCount,
  previousStep,
  nextStep,
  renderExtraOptions,
  disablePrevious,
  disableNext,
  onCancel,
  onFinish,
  loading
}) => {
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
          buttonText={<LeftOutlined />}
        />
      </div>
      <div className="ContinueButtons">
        {renderExtraOptions && <div className="SaveButton">{renderExtraOptions()}</div>}
        <CustomButton
          buttonProps={{
            onClick: lastStep ? onFinish : nextStep,
            className: 'theme-primary',
            disabled: disableNext
          }}
          buttonText={<RightOutlined />}
          loading={lastStep && loading}
        />
      </div>
    </div>
  );
};

StepFooter.defaultProps = {
  renderExtraOptions: null,
  disablePrevious: false,
  disableNext: false,
  loading: false
};

StepFooter.propTypes = {
  currentStep: PropTypes.number.isRequired,
  stepCount: PropTypes.number.isRequired,
  previousStep: PropTypes.func.isRequired,
  nextStep: PropTypes.func.isRequired,
  renderExtraOptions: PropTypes.func,
  disablePrevious: PropTypes.bool,
  disableNext: PropTypes.bool,
  loading: PropTypes.bool,
  onCancel: PropTypes.func.isRequired,
  onFinish: PropTypes.func.isRequired
};

export default StepFooter;
