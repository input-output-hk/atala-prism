import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import _ from 'lodash';
import ImportDataContainer from '../importContactData/ImportDataContainer';
import { withRedirector } from '../providers/withRedirector';
import { withApi } from '../providers/withApi';
import { COMMON_CREDENTIALS_HEADERS, IMPORT_CREDENTIALS_DATA } from '../../helpers/constants';
import { validateCredentialDataBulk } from '../../helpers/credentialDataValidation';
import { contactMapper } from '../../APIs/helpers/contactHelpers';
import { translateBackSpreadsheetNamesToContactKeys } from '../../helpers/contactValidations';
import { credentialTypeShape } from '../../helpers/propShapes';

const ImportCredentialsData = ({
  selectedGroups,
  selectedSubjects,
  subjects,
  credentialType,
  onCancel,
  onFinish,
  getContactsFromGroups
}) => {
  const { t } = useTranslation();

  const getTargetsData = async () => {
    const targetsFromGroups = selectedGroups.length ? (await getContactsFromGroups()).flat() : [];
    const targetsFromGroupsWithKeys = targetsFromGroups.map(contactMapper);

    const cherryPickedSubjects = subjects.filter(({ contactid }) =>
      selectedSubjects.includes(contactid)
    );

    const targetSubjects = [...targetsFromGroupsWithKeys, ...cherryPickedSubjects];
    const noRepeatedTargets = _.uniqBy(targetSubjects, 'externalid');

    return {
      contacts: noRepeatedTargets,
      credentialType
    };
  };

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

  const unapplyTranslationToKeys = (dataObjects, _groups, setResults) => {
    const untrasnlatedCredentials = translateBackSpreadsheetNamesToContactKeys(
      dataObjects,
      headersMapping
    );

    onFinish(untrasnlatedCredentials, setResults);
  };

  // append credential type to validator function arguments
  const validatorWithCredentialType = (credentialsData, headers) =>
    validateCredentialDataBulk(credentialType, credentialsData, headers, headersMapping);

  return (
    <ImportDataContainer
      getTargets={getTargetsData}
      bulkValidator={validatorWithCredentialType}
      onFinish={unapplyTranslationToKeys}
      onCancel={onCancel}
      useCase={IMPORT_CREDENTIALS_DATA}
      headersMapping={headersMapping}
    />
  );
};

ImportCredentialsData.propTypes = {
  selectedGroups: PropTypes.arrayOf(PropTypes.string).isRequired,
  selectedSubjects: PropTypes.arrayOf(PropTypes.string).isRequired,
  subjects: PropTypes.arrayOf(PropTypes.shape({ id: PropTypes.string })).isRequired,
  credentialType: PropTypes.shape(credentialTypeShape).isRequired,
  onCancel: PropTypes.func.isRequired,
  onFinish: PropTypes.func.isRequired,
  getContactsFromGroups: PropTypes.func.isRequired
};

export default withApi(withRedirector(ImportCredentialsData));
