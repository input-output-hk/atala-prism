import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import WaitBannerImage from '../../../../images/wait-banner.svg';
import { getThemeByRole } from '../../../../helpers/themeHelper';
import { useSession } from '../../../providers/SessionContext';

import './_style.scss';

const WaitBanner = () => {
  const { t } = useTranslation();
  const { session } = useSession();
  const theme = getThemeByRole(session.userRole);

  return (
    <div className={`WaitBannerContainer ${theme.class()}`}>
      <div className="WaitBannerText">
        <h2 className={theme.class()}>{t('dashboard.waitbanner.title')}</h2>
        <p className={theme.class()}>
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
