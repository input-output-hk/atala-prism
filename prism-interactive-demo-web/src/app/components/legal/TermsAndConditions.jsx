import React from 'react';
import { useTranslation } from 'react-i18next';

import './_style.scss';

const TermsAndConditions = () => {
  const { t } = useTranslation();

  return (
    <div className="LegalContainer">
      <div className="LogoContent">
        <img src="/images/logo-atala-prism.svg" alt={t('atalaLogo')} />
      </div>
      <div className="LegalContent">
        <div className="LegalHeader">
          <h1>{t('legal.termsAndConditions.title')}</h1>
          <p>{t('legal.termsAndConditions.lastUpdate')}</p>
        </div>
        <hr />
        <h3>1.{t('legal.termsAndConditions.title1')}</h3>
        <p>{t('legal.termsAndConditions.text1')}</p>
        <h3>2.{t('legal.termsAndConditions.title1')}</h3>
        <p>{t('legal.termsAndConditions.text1')}</p>
        <h3>3.{t('legal.termsAndConditions.title1')}</h3>
        <p>{t('legal.termsAndConditions.text1')}</p>
      </div>
    </div>
  );
};

export default TermsAndConditions;
