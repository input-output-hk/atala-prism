import React from 'react';
import { useTranslation } from 'react-i18next';
import { PulseLoader } from 'react-spinners';
import logo from '../../../../images/atala-logo-loading.svg';

import './_style.scss';

const altTextWhenI18nIsNotYetLoaded = 'Atala Logo';

const Loading = () => {
  const { t, ready } = useTranslation('ns1', { useSuspense: false });
  const altText = ready ? t('loading.logoName') : altTextWhenI18nIsNotYetLoaded;

  return (
    <div className="LoadingContent">
      <img src={logo} alt={altText} />
      <PulseLoader loading size={12} color="#000000" />
    </div>
  );
};
export default Loading;
