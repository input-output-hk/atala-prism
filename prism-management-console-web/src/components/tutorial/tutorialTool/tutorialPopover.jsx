import React from 'react';
import { useTranslation } from 'react-i18next';
import { Popover } from 'antd';
import './_style.scss';
import DashboardCard from '../../dashboard/organism/DashboardCard';
import CustomButton from '../../common/Atoms/CustomButton/CustomButton';

const TutorialPopover = () => {
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
  return (
    <Popover placement="right" content={content}>
      <div className="step1">
        <DashboardCard />
      </div>
    </Popover>
  );
};

export default TutorialPopover;
