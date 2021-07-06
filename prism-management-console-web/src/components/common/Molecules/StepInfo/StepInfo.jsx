import React from 'react';
import PropTypes from 'prop-types';
import { Badge } from 'antd';

import './_style.scss';

const StepInfo = ({ steps, currentStep }) => (
  <div className="StepsInfo">
    <div className="NewCredentialSteps">
      {steps.map(({ stepTitle }, index) => (
        <div key={stepTitle} className="StepLine">
          <div
            className={currentStep === index ? 'StepLineContentActive' : 'StepLineContent'}
            key={stepTitle}
          >
            <Badge
              style={
                currentStep === index
                  ? { backgroundColor: '#1ED69E' }
                  : { backgroundColor: '#fff', color: '#999' }
              }
              count={index + 1}
            />
          </div>
        </div>
      ))}
    </div>
  </div>
);

StepInfo.propTypes = {
  steps: PropTypes.arrayOf(
    PropTypes.shape({
      stepTitle: PropTypes.string.isRequired
    })
  ).isRequired,
  currentStep: PropTypes.number.isRequired
};

export default StepInfo;
