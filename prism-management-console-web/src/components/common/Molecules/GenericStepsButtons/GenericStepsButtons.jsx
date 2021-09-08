import React from 'react';
import PropTypes from 'prop-types';
import { Steps } from 'antd';
import { useTranslation } from 'react-i18next';
import { LeftOutlined, RightOutlined } from '@ant-design/icons';
import CustomButton from '../../Atoms/CustomButton/CustomButton';

import './_style.scss';

const { Step } = Steps;

const GenericStepsButtons = ({ steps, currentStep, disableBack, disableNext, loading }) => {
  const { t } = useTranslation();
  const { back, next } = steps[currentStep];
  return (
    <div className="GenericStepsButtons">
      <div className="ControlButtons">
        <CustomButton
          buttonProps={{
            onClick: back,
            className: 'theme-link',
            disabled: disableBack || !back
          }}
          buttonText={[
            <LeftOutlined key="0" />,
            <React.Fragment key="1">{t('actions.back')}</React.Fragment>
          ]}
        />
      </div>
      <div className="stepsContainer">
        <Steps size="small" current={currentStep}>
          {steps.map(({ key, title }) => (
            <Step key={key} title={title} />
          ))}
        </Steps>
      </div>
      <div className="ControlButtons">
        <CustomButton
          buttonProps={{
            onClick: next,
            className: 'theme-link',
            disabled: disableNext || !next
          }}
          buttonText={[
            <React.Fragment key="2">{t('actions.next')}</React.Fragment>,
            <RightOutlined key="3" />
          ]}
          loading={loading}
        />
      </div>
    </div>
  );
};

GenericStepsButtons.defaultProps = {
  disableBack: false,
  disableNext: false,
  loading: false
};

GenericStepsButtons.propTypes = {
  steps: PropTypes.arrayOf(
    PropTypes.shape({ title: PropTypes.string, back: PropTypes.func, next: PropTypes.func })
  ).isRequired,
  currentStep: PropTypes.number.isRequired,
  disableBack: PropTypes.bool,
  disableNext: PropTypes.bool,
  loading: PropTypes.bool
};

export default GenericStepsButtons;
