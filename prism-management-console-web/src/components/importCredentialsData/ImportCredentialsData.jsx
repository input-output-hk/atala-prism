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

const ImportCredentialsData = ({ recipients, credentialType, onCancel, onFinish }) => {
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

  const handleFinishUpload = (credentials, _groups, setResults) => {
    onFinish(credentials, setResults);
  };

  // append credential type to validator function arguments
  const validatorWithCredentialType = (credentialsData, headers) =>
    validateCredentialDataBulk(
      credentialType,
      credentialsData,
      headers,
      headersMapping,
      recipients
    );

  return (
    <ImportDataContainer
      recipients={recipients}
      credentialType={credentialType}
      bulkValidator={validatorWithCredentialType}
      onFinish={handleFinishUpload}
      onCancel={onCancel}
      useCase={IMPORT_CREDENTIALS_DATA}
      headersMapping={headersMapping}
    />
  );
};

ImportCredentialsData.propTypes = {
  recipients: PropTypes.arrayOf(PropTypes.shape(contactShape)).isRequired,
  credentialType: PropTypes.shape(credentialTypeShape).isRequired,
  onCancel: PropTypes.func.isRequired,
  onFinish: PropTypes.func.isRequired
};

export default withApi(withRedirector(ImportCredentialsData));
