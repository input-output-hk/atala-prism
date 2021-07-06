import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import RecipientsList from '../../Molecules/RecipientsList/RecipientsList';
import CredentialsViewer from '../../Molecules/CredentialsViewer/CredentialsViewer';
import './_style.scss';

const CredentialsPreview = ({ credentialViews, groups, subjects }) => {
  const { t } = useTranslation();

  return (
    <div className="CredWrapper">
      <div className="CredentialsPreview">
        <CredentialsViewer credentialViews={credentialViews} />
        <div className="Divider" />
        <div className="RecipientsContainer">
          <h3>{t('newCredential.credentialsPreview.title')}</h3>
          <h4>{t('newCredential.credentialsPreview.subtitle')}</h4>
          <RecipientsList recipients={groups.concat(subjects)} />
        </div>
      </div>
    </div>
  );
};

CredentialsPreview.propTypes = {
  credentialViews: PropTypes.arrayOf(PropTypes.string).isRequired,
  groups: PropTypes.arrayOf(PropTypes.shape({ name: PropTypes.string })).isRequired,
  subjects: PropTypes.arrayOf(
    PropTypes.shape({
      contactId: PropTypes.string,
      contactName: PropTypes.string,
      externalId: PropTypes.string,
      creationDate: PropTypes.shape({
        day: PropTypes.number,
        month: PropTypes.number,
        year: PropTypes.number
      })
    })
  ).isRequired
};

export default CredentialsPreview;
