import React from 'react';
import PropTypes from 'prop-types';
import { Steps } from 'antd';
import { useTranslation } from 'react-i18next';
import { LeftOutlined, RightOutlined } from '@ant-design/icons';
import CustomButton from '../../Atoms/CustomButton/CustomButton';

import './_style.scss';

/*

  @dbrosio ya no vamos a usar mas los steps que están entre los botones de back y next. Podrìas sacar esa lógica para limpiar el codigo?

const { Step } = Steps;

*/

const GenericStepsButtons = ({ steps, currentStep, disableBack, disableNext, loading }) => {
  const { t } = useTranslation();
  const { back, next } = steps[currentStep];

  const nextButton = (
    <>
      {t('actions.next')}
      <RightOutlined />
    </>
  );

  const backButton = (
    <>
      <LeftOutlined />
      {t('actions.back')}
    </>
  );

  return (
    <div className="GenericStepsButtons">
      <div className="ControlButtons">
        <CustomButton
          buttonProps={{
            onClick: back,
            className: 'theme-link',
            disabled: disableBack || !back
          }}
          buttonText={backButton}
        />
      </div>
      <div className="ControlButtons">
        <CustomButton
          buttonProps={{
            onClick: next,
            className: 'theme-link',
            disabled: disableNext || !next
          }}
          buttonText={nextButton}
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
