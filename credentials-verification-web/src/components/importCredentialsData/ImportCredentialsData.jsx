import React from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';
import ImportDataContainer from '../importContactData/ImportDataContainer';
import { withRedirector } from '../providers/withRedirector';
import { withApi } from '../providers/withApi';
import { IMPORT_CREDENTIALS_DATA } from '../../helpers/constants';
import { validateCredentialDataBulk } from '../../helpers/credentialDataValidation';
import { contactMapper } from '../../APIs/helpers';

const ImportCredentialsData = ({
  selectedGroups,
  selectedSubjects,
  subjects,
  credentialType,
  onCancel,
  onFinish,
  getContactsFromGroups
}) => {
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

  // TODO: to be implemented along with credentials data validation
  const headersMapping = [];

  return (
    <ImportDataContainer
      getTargets={getTargetsData}
      bulkValidator={validateCredentialDataBulk}
      onFinish={onFinish}
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
  credentialType: PropTypes.shape({ name: PropTypes.string }).isRequired,
  onCancel: PropTypes.func.isRequired,
  onFinish: PropTypes.func.isRequired,
  getContactsFromGroups: PropTypes.func.isRequired
};

export default withApi(withRedirector(ImportCredentialsData));
