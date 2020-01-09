import React from 'react';
import { useTranslation } from 'react-i18next';
import TrustItem from '../../Atoms/TrustItem/TrustItem';
import DownloadButtons from '../../Molecules/DownloadButtons/DownloadButtons';

import './_style.scss';

const TrustSection = () => {
  const { t } = useTranslation();

  return (
    <div className="TrustSection">
      <div className="TrustItemContainer">
        <TrustItem />
        <TrustItem />
      </div>
      <div className="DownloadContainer">
        <h1>{t('landing.trust.predownloadText')}</h1>
        <DownloadButtons />
      </div>
      <img
        src="images/trust-bg.png"
        alt="Verified Icon"
        className={t('landing.trust.mobilePhones')}
      />
    </div>
  );
};

export default TrustSection;
