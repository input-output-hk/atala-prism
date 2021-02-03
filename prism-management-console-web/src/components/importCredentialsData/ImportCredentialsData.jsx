import React from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';
import { useTranslation } from 'react-i18next';
import ImportDataContainer from '../importContactData/ImportDataContainer';
import { withRedirector } from '../providers/withRedirector';
import { withApi } from '../providers/withApi';
import { COMMON_CREDENTIALS_HEADERS, IMPORT_CREDENTIALS_DATA } from '../../helpers/constants';
import { validateCredentialDataBulk } from '../../helpers/credentialDataValidation';
import { contactShape, credentialTypeShape } from '../../helpers/propShapes';

const ImportCredentialsData = ({
  recipients,
  contacts,
  credentialType,
  onCancel,
  onFinish,
  hasSelectedRecipients
}) => {
  const { t } = useTranslation();

  const generateHeadersMapping = () => {
    const credentialTypeHeaders = credentialType.fields.map(f => f.key);

    const compiledHeaders = [...COMMON_CREDENTIALS_HEADERS, ...credentialTypeHeaders];

    const noRepeatedHeaders = _.uniq(compiledHeaders);

    return noRepeatedHeaders.map(headerKey => ({
      key: headerKey,
      translation: t(`contacts.table.columns.${headerKey}`)
    }));
  };

  const headersMapping = generateHeadersMapping();

  // append credential type to validator function arguments
  const validatorWithCredentialType = (credentialsData, headers) =>
    validateCredentialDataBulk(
      credentialType,
      credentialsData,
      headers,
      headersMapping,
      recipients,
      contacts
    );

  return (
    <ImportDataContainer
      recipients={recipients}
      credentialType={credentialType}
      bulkValidator={validatorWithCredentialType}
      onFinish={onFinish}
      onCancel={onCancel}
      useCase={IMPORT_CREDENTIALS_DATA}
      headersMapping={headersMapping}
      hasSelectedRecipients={hasSelectedRecipients}
    />
  );
};

ImportCredentialsData.defaultProps = {
  hasSelectedRecipients: false
};

ImportCredentialsData.propTypes = {
  recipients: PropTypes.arrayOf(PropTypes.shape(contactShape)).isRequired,
  contacts: PropTypes.arrayOf(PropTypes.shape(contactShape)).isRequired,
  credentialType: PropTypes.shape(credentialTypeShape).isRequired,
  onCancel: PropTypes.func.isRequired,
  onFinish: PropTypes.func.isRequired,
  hasSelectedRecipients: PropTypes.bool
};

export default withApi(withRedirector(ImportCredentialsData));
