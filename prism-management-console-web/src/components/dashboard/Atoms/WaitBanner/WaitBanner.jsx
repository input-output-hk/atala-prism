import React from 'react';
import { useTranslation } from 'react-i18next';
import WaitBannerImage from '../../../../images/wait-banner.svg';

import './_style.scss';

const WaitBanner = () => {
  const { t } = useTranslation();

  return (
    <div className="WaitBannerContainer">
      <div className="WaitBannerText">
        <h2>{t('dashboard.waitbanner.title')}</h2>
        <p>
          {t('dashboard.waitbanner.paragraph')}
          <strong>{t('dashboard.waitbanner.delayTime')}</strong>
        </p>
      </div>
      <div className="WaitBannerImg">
        <img src={WaitBannerImage} alt={t('dashboard.waitbanner.image')} />
      </div>
    </div>
  );
};

export default WaitBanner;
