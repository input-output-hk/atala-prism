import React from 'react';

import './_style.scss';
import { useTranslation } from 'react-i18next';

const CredentialSection = () => {
  const { t } = useTranslation();

  return (
    <div className="CredentialSection">
      <div className="ImageContainer">
        <img src="images/credential-example.svg" alt={t('landing.credential.credentialAlt')} />
      </div>
      <div className="TextContainer">
        <span className="MiniDetailText">
          {t('landing.credential.detailText')}
          <em>_____</em>
        </span>
        <h1>{t('landing.credential.title')}</h1>
        <h3>
          {t('landing.credential.subtitle1')}
          {t('landing.credential.subtitle2')}
        </h3>
      </div>
    </div>
  );
};

export default CredentialSection;
