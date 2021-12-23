import React from 'react';
import { useTranslation } from 'gatsby-plugin-react-i18next';

import './_style.scss';

export const items = [
  { title: 'landing.trust.titlebenefit1', description: 'landing.trust.benefit1' },
  { title: 'landing.trust.titlebenefit2', description: 'landing.trust.benefit2' },
  { title: 'landing.trust.titlebenefit3', description: 'landing.trust.benefit3' },
  { title: 'landing.trust.titlebenefit4', description: 'landing.trust.benefit4' },
  { title: 'landing.trust.titlebenefit5', description: 'landing.trust.benefit5' },
  { title: 'landing.trust.titlebenefit6', description: 'landing.trust.benefit6' }
];

const TrustSection = () => {
  const { t } = useTranslation();

  return (
    <div className="TrustSection">
      <div className="DownloadContainer">
        <h1>{t('landing.trust.predownloadText')}</h1>
      </div>
      <div className="BenefitsContainer">
        {items.map(({ title, description }) => (
          <div className="BenefitItem" key={title}>
            <h3>{t(title)}</h3>
            <p>{t(description)}</p>
          </div>
        ))}
      </div>
    </div>
  );
};

export default TrustSection;
