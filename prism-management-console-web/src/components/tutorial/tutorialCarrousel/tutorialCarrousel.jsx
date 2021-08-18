import React, { useState } from 'react';
import { Carousel } from 'antd';
import { useTranslation } from 'react-i18next';
import './_style.scss';
import onboarding1 from '../../../images/onboarding1.png';
import onboarding2 from '../../../images/onboarding2.png';
import onboarding3 from '../../../images/onboarding3.png';
import CustomButton from '../../common/Atoms/CustomButton/CustomButton';

function onChange(a, b, c) {
  console.log(a, b, c);
}

const TutorialCarrousel = () => {
  const { t } = useTranslation();
  return (
    <Carousel className="tutorialOnboarding" afterChange={onChange}>
      <div className="contentStyle">
        <img src={onboarding1} alt="onboarding" />
        <h2>{t('tutorial.onboarding.title')}</h2>
        <p>{t('tutorial.onboarding.description')}</p>
      </div>
      <div className="contentStyle">
        <img style={{ marginTop: 20, width: 220 }} src={onboarding2} alt="onboarding" />
        <h2>{t('tutorial.onboarding.titleTwo')}</h2>
        <p>{t('tutorial.onboarding.descriptionTwo')}</p>
      </div>
      <div className="contentStyle">
        <img style={{ height: 190 }} src={onboarding3} alt="onboarding" />
        <h2>{t('tutorial.onboarding.titleThree')}</h2>
        <p>{t('tutorial.onboarding.descriptionThree')}</p>
        <CustomButton
          buttonProps={{
            className: 'theme-secondary'
          }}
          buttonText="Start Tutorial"
        />
      </div>
    </Carousel>
  );
};

export default TutorialCarrousel;
