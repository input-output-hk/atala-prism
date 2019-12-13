import React from 'react';
import { PulseLoader } from 'react-spinners';
import { useTranslation } from 'react-i18next';
import logo from '../../../../images/landingLogo.svg';

import './_style.scss';

const Loading = () => {
  const { t } = useTranslation();
  return (
    <div className="LoadingContent">
      <img src={logo} alt={t('loading.logoName')} />
      <PulseLoader loading size={15} color="#000000" />
    </div>
  );
};
export default Loading;
