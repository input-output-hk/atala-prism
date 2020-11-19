import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import CellRenderer from '../../../common/Atoms/CellRenderer/CellRenderer';
import { shortDateFormatter } from '../../../../helpers/formatters';

import './_style.scss';
import { useTranslationWithPrefix } from '../../../../hooks/useTranslationWithPrefix';

const translationPrefix = 'newCredential.table.columns';

const NewCredentialValidation = ({
  credentialValues: { startDate, degreeName, logoUniversity },
  group: { name }
}) => {
  const { t } = useTranslation();
  const tp = useTranslationWithPrefix(translationPrefix);

  return (
    <div className="NewCredentialValidationContainer">
      <h2>{t('newCredential.checkInfo')}</h2>
      <div className="NewCredentialValidation">
        <img
          style={{ height: '50px', width: '50px' }}
          src="icon-free-university.svg"
          alt={t('newCredential.form.logoUniversity')}
        />
        <h3>{degreeName}</h3>
        <CellRenderer title={tp('date')} value={shortDateFormatter(startDate)} />
        <CellRenderer title={tp('groupAssigned')} value={name} />
      </div>
    </div>
  );
};

NewCredentialValidation.propTypes = {
  credentialValues: PropTypes.shape({
    startDate: PropTypes.number,
    degreeName: PropTypes.string,
    logoUniversity: PropTypes.shape()
  }).isRequired,
  group: PropTypes.string.isRequired
};

export default NewCredentialValidation;
