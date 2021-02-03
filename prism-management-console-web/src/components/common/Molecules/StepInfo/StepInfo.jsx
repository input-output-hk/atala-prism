import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { Badge, Col, Row } from 'antd';

import './_style.scss';

const StepInfo = ({ title, subtitle, steps, currentStep }) => {
  const { t } = useTranslation();

  return (
    <Row type="flex" align="middle" className="StepsInfo">
      <Col xs={24} lg={12} className="NewCredentialTitle">
        {title && <h1>{t(title)}</h1>}
        {subtitle && <p>{t(subtitle)}</p>}
      </Col>
      <Col xs={24} lg={24} className="NewCredentialSteps">
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
      </Col>
    </Row>
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
