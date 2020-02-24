import React, { useContext } from 'react';
import { useTranslation } from 'react-i18next';
import CredentialData from '../../../common/Atoms/CredentialData/CredentialData';
import { UserContext } from '../../../providers/userContext';

import './_style.scss';
import CredentialIDTemplate from '../../Molecules/CredentialIdTemplate/CredentialIdTemplate';

const CreatedCredential = () => {
  const { t } = useTranslation();
  const { user } = useContext(UserContext);

  return (
    <div className="CreatedCredential">
      <h3>{t('landing.createdCredentialGov.Title')}</h3>
      <div className="ContainerCredential">
        {/* <CredentialData {...user} /> */}
        <CredentialIDTemplate />
      </div>
    </div>
  );
};

export default CreatedCredential;
