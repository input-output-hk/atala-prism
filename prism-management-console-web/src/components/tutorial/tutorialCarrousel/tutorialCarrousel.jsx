import React, { useRef } from 'react';
import PropTypes from 'prop-types';
import { Carousel } from 'antd';
import { useTranslation } from 'react-i18next';
import { LeftOutlined, RightOutlined } from '@ant-design/icons';
import onboarding1 from '../../../images/onboarding1.png';
import onboarding2 from '../../../images/onboarding2.png';
import onboarding3 from '../../../images/onboarding3.png';
import CustomButton from '../../common/Atoms/CustomButton/CustomButton';

import './_style.scss';

const TutorialCarrousel = ({ onStart }) => {
  const { t } = useTranslation();
  const carousel = useRef();

  const next = () => carousel.current?.next();
  const prev = () => carousel.current?.prev();

  return (
    <Carousel className="tutorialOnboarding" ref={carousel}>
      <div className="containerStyle">
        <div className="noButton" />
        <div className="contentStyle">
          <img src={onboarding1} alt="onboarding" />
          <h2>{t('tutorial.onboarding.title')}</h2>
          <p>{t('tutorial.onboarding.description')}</p>
        </div>
        <CustomButton
          buttonProps={{
            className: 'theme-text',
            onClick: next
          }}
          className="navButton"
          buttonText={<RightOutlined />}
        />
      </div>
      <div className="containerStyle">
        <CustomButton
          buttonProps={{
            className: 'theme-text',
            onClick: prev
          }}
          className="navButton"
          buttonText={<LeftOutlined />}
        />
        <div className="contentStyle">
          <img style={{ marginTop: 20, width: 220 }} src={onboarding2} alt="onboarding" />
          <h2>{t('tutorial.onboarding.titleTwo')}</h2>
          <p>{t('tutorial.onboarding.descriptionTwo')}</p>
        </div>
        <CustomButton
          buttonProps={{
            className: 'theme-text',
            onClick: next
          }}
          className="navButton"
          buttonText={<RightOutlined />}
        />
      </div>
      <div className="containerStyle">
        <CustomButton
          buttonProps={{
            className: 'theme-text',
            onClick: prev
          }}
          className="navButton"
          buttonText={<LeftOutlined />}
        />
        <div className="contentStyle">
          <img style={{ height: 190 }} src={onboarding3} alt="onboarding" />
          <h2>{t('tutorial.onboarding.titleThree')}</h2>
          <p>{t('tutorial.onboarding.descriptionThree')}</p>
          <CustomButton
            buttonProps={{
              className: 'theme-secondary',
              onClick: onStart
            }}
            buttonText={t('tutorial.onboarding.buttonText.continue')}
          />
        </div>
        <div className="noButton" />
      </div>
    </Carousel>
  );
};

TutorialCarrousel.propTypes = {
  onStart: PropTypes.func.isRequired
};

export default TutorialCarrousel;
