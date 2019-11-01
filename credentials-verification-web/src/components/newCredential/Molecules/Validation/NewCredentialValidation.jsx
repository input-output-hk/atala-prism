import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import CellRenderer from '../../../common/Atoms/CellRenderer/CellRenderer';
import { shortDateFormatter } from '../../../../helpers/formatters';

import './_style.scss';

const NewCredentialValidation = ({
  credentialValues: { startDate, degreeName, logoUniversity },
  group
}) => {
  const { t } = useTranslation();
  const identityNumber = '3';

  return (
    <div className="NewCredentialValidationContainer">
      <h2>{t('newCredential.validation.title')}</h2>
      <div className="NewCredentialValidation">
        <img
          style={{ height: '50px', width: '50px' }}
          src="icon-free-university.svg"
          alt={t('newCredential.form.logoUniversity')}
        />
        <h3>{degreeName}</h3>
        <CellRenderer title="identityNumber" value={identityNumber} componentName="newCredential" />
        <CellRenderer
          title="date"
          value={shortDateFormatter(startDate / 1000)}
          componentName="newCredential"
        />
        <CellRenderer title="groupAssigned" value={group} componentName="newCredential" />
      </div>
    </div>
  );
};

NewCredentialValidation.propTypes = {
  credentialValues: PropTypes.shape({
    startDate: PropTypes.number,
    degreeName: PropTypes.string,
    logoUniversity: PropTypes.any
  }).isRequired,
  group: PropTypes.string.isRequired
};

export default NewCredentialValidation;
