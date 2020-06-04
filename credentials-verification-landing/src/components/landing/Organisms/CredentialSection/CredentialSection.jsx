import React from 'react';

import './_style.scss';
import { useTranslation } from 'react-i18next';

const CredentialSection = () => {
  const { t } = useTranslation();

  return (
    <div className="ComponentsPanel">
      <div className="CredentialSection">
        <div className="TextContainer">
          <h1>{t('landing.credential.title')}</h1>
          <div className="ComponentsContainer">
            <div className="ComponentItem">
              <h3>{t('landing.credential.titlecomponent1')}</h3>
              <p>{t('landing.credential.component1')}</p>
            </div>
            <div className="ComponentItem">
              <h3>{t('landing.credential.titlecomponent3')}</h3>
              <p>{t('landing.credential.component3')}</p>
            </div>
            <div className="ComponentItem">
              <h3>{t('landing.credential.titlecomponent2')}</h3>
              <p>{t('landing.credential.component2')}</p>
            </div>
            <div className="ComponentItem">
              <h3>{t('landing.credential.titlecomponent4')}</h3>
              <p>{t('landing.credential.component4')}</p>
            </div>
            <div className="ComponentItem">
              <h3>{t('landing.credential.titlecomponent5')}</h3>
              <p className="ComingSoon">{t('landing.credential.title7')}</p>
              <p>{t('landing.credential.component5')}</p>
            </div>
            <div className="ComponentItem">
              <h3>{t('landing.credential.titlecomponent6')}</h3>
              <p className="ComingSoon">{t('landing.credential.title7')}</p>
              <p>{t('landing.credential.component6')}</p>
            </div>
          </div>
        </div>
        <div className="ImageContainer">
          <img
            src="images/illustration-components.svg"
            alt={t('landing.credential.credentialAlt')}
          />
        </div>
      </div>
      <div className="triangle-down" />
    </div>
  );
};

export default CredentialSection;
