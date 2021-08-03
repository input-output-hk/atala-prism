import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import { withApi } from '../providers/withApi';
import ManualImport from './ManualImport';
import Logger from '../../helpers/Logger';
import { IMPORT_CONTACTS, IMPORT_CREDENTIALS_DATA } from '../../helpers/constants';
import { contactCreationShape, credentialTypeShape, groupShape } from '../../helpers/propShapes';

const ManualImportContainer = ({
  api: { groupsManager },
  useCaseProps,
  credentialType,
  credentialsData,
  selectedGroups,
  setSelectedGroups,
  addEntity
}) => {
  const { t } = useTranslation();
  const [groups, setGroups] = useState([]);
  const { showGroupSelection } = useCaseProps;

  useEffect(() => {
    if (showGroupSelection) {
      groupsManager
        .getGroups({})
        .then(({ groupsList }) => setGroups(groupsList))
        .catch(error => {
          Logger.error('[GroupsContainer.updateGroups] Error: ', error);
          message.error(t('errors.errorGetting', { model: t('groups.title') }));
        });
    }
  }, [groupsManager, showGroupSelection, t]);

  const groupsProps = { groups, selectedGroups, setSelectedGroups };

  return (
    <ManualImport
      addEntity={addEntity}
      initialValues={credentialsData}
      groupsProps={groupsProps}
      credentialType={credentialType}
      {...useCaseProps}
    />
  );
};

ManualImportContainer.defaultProps = {
  credentialType: {}
};

ManualImportContainer.propTypes = {
  api: PropTypes.shape({
    groupsManager: PropTypes.shape({
      getGroups: PropTypes.func.isRequired
    }).isRequired
  }).isRequired,
  useCaseProps: PropTypes.shape({
    useCase: PropTypes.oneOf([IMPORT_CONTACTS, IMPORT_CREDENTIALS_DATA]).isRequired,
    showGroupSelection: PropTypes.func.isRequired,
    isEmbedded: PropTypes.bool.isRequired
  }).isRequired,
  credentialType: PropTypes.shape(credentialTypeShape),
  hasSelectedRecipients: PropTypes.bool.isRequired,
  setContacts: PropTypes.func.isRequired,
  credentialsData: PropTypes.shape(contactCreationShape).isRequired,
  setCredentialsData: PropTypes.func.isRequired,
  selectedGroups: PropTypes.arrayOf(PropTypes.shape(groupShape)).isRequired,
  setSelectedGroups: PropTypes.func.isRequired,
  addEntity: PropTypes.func.isRequired
};

export default withApi(ManualImportContainer);
