import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { Badge } from 'antd';

import './_style.scss';

const StepInfo = ({ title, subtitle, steps, currentStep }) => {
  const { t } = useTranslation();

  return (
    <div className="StepsInfo">
      <div className="NewCredentialTitle">
        {title && <h1>{t(title)}</h1>}
        {subtitle && <p>{t(subtitle)}</p>}
      </div>
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
                count={t('generic.step', { step: index + 1 })}
              />
              {/* eslint-disable-next-line jsx-a11y/label-has-for */}
              <label id="stepTitle">{t(stepTitle)}</label>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

StepInfo.defaultProps = {
  title: '',
  subtitle: ''
};

StepInfo.propTypes = {
  title: PropTypes.string,
  subtitle: PropTypes.string,
  steps: PropTypes.arrayOf(
    PropTypes.shape({
      stepTitle: PropTypes.string.isRequired
    })
  ).isRequired,
  currentStep: PropTypes.number.isRequired
};

export default StepInfo;
