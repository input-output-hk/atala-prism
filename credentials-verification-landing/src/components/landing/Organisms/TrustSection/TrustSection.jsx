import React from 'react';
import { useTranslation } from 'react-i18next';
import DownloadButtons from '../../Molecules/DownloadButtons/DownloadButtons';

import './_style.scss';

const TrustSection = () => {
  const { t } = useTranslation();

  return (
    <div className="TrustSection">
      <div className="DownloadContainer">
        <span className="MiniDetailText">
          {t('landing.trust.downloadInfo')}
          <em>_____</em>
        </span>
        <h1>{t('landing.trust.predownloadText')}</h1>
        <DownloadButtons />
      </div>
      <img src="images/trust-bg.png" alt="Verified Icon" className="MobilePhones" />
    </div>
  );
};

export default TrustSection;
