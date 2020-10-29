import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import RecipientsList from '../../Molecules/RecipientsList/RecipientsList';
import CredentialsViewer from '../../Molecules/CredentialsViewer/CredentialsViewer';

import './_style.scss';

const CredentialsPreview = ({
  credentialViewTemplate,
  groups,
  subjects,
  credentialsData,
  credentialPlaceholders
}) => {
  const { t } = useTranslation();

  return (
    <div className="CredentialsPreview">
      <CredentialsViewer
        credentialViewTemplate={credentialViewTemplate}
        credentialsData={credentialsData}
        credentialPlaceholders={credentialPlaceholders}
      />
      <div className="Divider" />
      <div className="RecipientsContainer">
        <h3>{t('newCredential.credentialsPreview.title')}</h3>
        <h4>{t('newCredential.credentialsPreview.subtitle')}</h4>
        <RecipientsList recipients={groups.concat(subjects)} />
      </div>
    </div>
  );
};

CredentialsPreview.propTypes = {
  credentialViewTemplate: PropTypes.shape({
    id: PropTypes.string,
    name: PropTypes.string,
    encodedlogoimage: PropTypes.string,
    logoimagemimetype: PropTypes.string,
    htmltemplate: PropTypes.string
  }).isRequired,
  credentialPlaceholders: PropTypes.objectOf(PropTypes.string).isRequired,
  groups: PropTypes.arrayOf(PropTypes.shape({ name: PropTypes.string })).isRequired,
  subjects: PropTypes.arrayOf(
    PropTypes.shape({
      contactid: PropTypes.string,
      contactName: PropTypes.string,
      externalId: PropTypes.string,
      creationDate: PropTypes.shape({
        day: PropTypes.number,
        month: PropTypes.number,
        year: PropTypes.number
      })
    })
  ).isRequired,
  credentialsData: PropTypes.arrayOf(PropTypes.objectOf(PropTypes.string)).isRequired
};

export default CredentialsPreview;
