import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import CredentialData from '../../../common/Atoms/CredentialData/CredentialData';

import './_style.scss';

const CreatedCredential = ({ credentialData }) => {
  const { t } = useTranslation();

  return (
    <div className="CreatedCredential">
      <h3>{t('credential.createdCredential.title')}</h3>
      <CredentialData {...credentialData} />
    </div>
  );
};

CreatedCredential.propTypes = {
  credentialData: PropTypes.shape().isRequired
};

export default CreatedCredential;
