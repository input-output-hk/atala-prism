import React from 'react';
import { useTranslation } from 'react-i18next';

import './_style.scss';

const GetStarted = () => {
  const { t } = useTranslation();

  return (
    <div className="GetStartedContent">
      <div className="TextContainer">
        <span className="MiniDetailText">{t('landing.getStarted.detailText')}</span>
        <div className="GetStartedDescription">
          <h1>{t('landing.getStarted.title')}</h1>
          <h3>{t('landing.getStarted.part1')}</h3>
          <h3>{t('landing.getStarted.part2')}</h3>
          <h3>{t('landing.getStarted.part3')}</h3>
        </div>
      </div>
    </div>
  );
};

export default GetStarted;
