import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Popover } from 'antd';
import CustomButton from '../../common/Atoms/CustomButton/CustomButton';
import './_style.scss';

const TutorialPopover = ({ visible, children }) => {
  const { t } = useTranslation();

  const content = (
    <div className="popOverStep1">
      <p className="steps">{t('tutorial.stepsBox.steps')}</p>
      <div className="popoverText">
        <p className="subtitle">{t('tutorial.stepsBox.section')}</p>
        <h2>{t('tutorial.stepsBox.stepName')}</h2>
        <p>{t('tutorial.stepsBox.description')}</p>
      </div>
      <div className="popoverButtons">
        <CustomButton
          buttonProps={{
            className: 'theme-outline'
          }}
          buttonText={t('tutorial.stepsBox.buttonText.back')}
        />
        <CustomButton
          buttonProps={{
            className: 'theme-primary'
          }}
          buttonText={t('tutorial.stepsBox.buttonText.next')}
        />
      </div>
    </div>
  );

  if (!visible) return children;

  return (
    <Popover overlayClassName="TutorialPopover" placement="right" content={content}>
      <div className="step1">{children}</div>
    </Popover>
  );
};

TutorialPopover.defaultProps = {
  visible: true,
  children: null
};

TutorialPopover.propTypes = {
  visible: PropTypes.bool,
  children: PropTypes.node
};

export default TutorialPopover;
