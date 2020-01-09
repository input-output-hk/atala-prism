import React from 'react';
import { useTranslation } from 'react-i18next';
import CredentialItem from '../../Molecules/CredentialItem/CredentialItem';

import './_style.scss';

const FindCredential = () => {
  const { t } = useTranslation();

  return (
    <div className="FindCredential">
      <div className="TextContainer">
        <h1>{t('landing.findCredential.title')}</h1>
        <img src="images/icon-arrow-right.svg" alt={t('landing.findCredential.arrowAlt')} />
      </div>
      <div className="FindCredentialContainer">
        <CredentialItem />
      </div>
    </div>
  );
};

export default FindCredential;
