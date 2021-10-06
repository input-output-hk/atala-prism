import React from 'react';
import { useTranslation } from 'gatsby-plugin-react-i18next';

import './_style.scss';

const TrustSection = () => {
  const { t } = useTranslation();

  return (
    <div className="TrustSection">
      <div className="DownloadContainer">
        <h1>{t('landing.trust.predownloadText')}</h1>
      </div>
      <div className="BenefitsContainer">
        <div className="BenefitItem">
          <h3>{t('landing.trust.titlebenefit1')}</h3>
          <p>{t('landing.trust.benefit1')}</p>
        </div>
        <div className="BenefitItem">
          <h3>{t('landing.trust.titlebenefit2')}</h3>
          <p>{t('landing.trust.benefit2')}</p>
        </div>
        <div className="BenefitItem">
          <h3>{t('landing.trust.titlebenefit3')}</h3>
          <p>{t('landing.trust.benefit3')}</p>
        </div>
        <div className="BenefitItem">
          <h3>{t('landing.trust.titlebenefit4')}</h3>
          <p>{t('landing.trust.benefit4')}</p>
        </div>
        <div className="BenefitItem">
          <h3>{t('landing.trust.titlebenefit5')}</h3>
          <p>{t('landing.trust.benefit5')}</p>
        </div>
        <div className="BenefitItem">
          <h3>{t('landing.trust.titlebenefit6')}</h3>
          <p>{t('landing.trust.benefit6')}</p>
        </div>
      </div>
    </div>
  );
};

export default TrustSection;
