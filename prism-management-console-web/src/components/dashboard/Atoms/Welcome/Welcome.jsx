import React from 'react';
import { useTranslation } from 'react-i18next';
import welcomeImage from '../../../../images/welcome-img.png';

import './_style.scss';

const Welcome = () => {
  const { t } = useTranslation();

  return (
    <div className="WelcomeContainer">
      <div className="WelcomeText">
        <h2>{t('dashboard.welcome.title')}</h2>
        <p>
          {t('dashboard.welcome.paragraph')}
          <strong>{t('dashboard.welcome.atalaPrism')}</strong>
        </p>
      </div>
      <div className="WelcomeImg">
        <img src={welcomeImage} alt={t('dashboard.welcome.image')} />
      </div>
    </div>
  );
};

export default Welcome;
