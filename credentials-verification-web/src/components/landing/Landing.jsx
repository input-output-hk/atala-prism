import React from 'react';
import { useTranslation } from 'react-i18next';
import { useHistory } from 'react-router-dom';
import CustomButton from '../common/Atoms/CustomButton/CustomButton';
import preloginLogo from '../../images/landingLogo.svg';

import './_style.scss';

const Landing = () => {
  const { t } = useTranslation();
  const history = useHistory();

  return (
    <div className="LandingContainer">
      <div className="LandingCard">
        <img src={preloginLogo} alt={t('landing.logoAlt')} />
        <div className="WelcomeText">
          <h3>{t('landing.welcome')}</h3>
        </div>
        <div className="LandingOptions">
          <CustomButton
            buttonProps={{
              className: 'theme-outline',
              onClick: () => history.push('/registration')
            }}
            buttonText={t('landing.register')}
          />
          <CustomButton
            buttonProps={{ className: 'theme-secondary', onClick: () => history.push('/login') }}
            buttonText={t('landing.login')}
          />
        </div>
      </div>
    </div>
  );
};

export default Landing;
