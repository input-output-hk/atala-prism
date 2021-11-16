import React from 'react';

import './_style.scss';
import { useTranslation } from 'gatsby-plugin-react-i18next';

const TrustItem = () => {
  const { t } = useTranslation();

  return (
    <div className="TrustItem">
      <img src="/images/icon-verified.svg" alt="Verified Icon" />
      <div className="TrustText">
        <h2>{t('landing.trust.trustName')}</h2>
        <p>{t('landing.trust.info')}</p>
      </div>
    </div>
  );
};

export default TrustItem;
