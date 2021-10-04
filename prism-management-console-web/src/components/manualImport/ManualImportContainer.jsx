import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import { withApi } from '../providers/withApi';
import ManualImport from './ManualImport';
import Logger from '../../helpers/Logger';
import { IMPORT_CONTACTS, IMPORT_CREDENTIALS_DATA } from '../../helpers/constants';
import { credentialShape, credentialTypeShape, groupShape } from '../../helpers/propShapes';

const ManualImportContainer = ({
  api: { groupsManager },
  useCaseProps,
  credentialType,
  recipients,
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
      recipients={recipients}
      groupsProps={groupsProps}
      credentialType={credentialType}
      {...useCaseProps}
    />
  );
};

ManualImportContainer.defaultProps = {
  credentialType: {},
  recipients: []
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
  credentialType: credentialTypeShape,
  hasSelectedRecipients: PropTypes.bool.isRequired,
  setContacts: PropTypes.func.isRequired,
  recipients: PropTypes.arrayOf(credentialShape),
  selectedGroups: PropTypes.arrayOf(PropTypes.shape(groupShape)).isRequired,
  setSelectedGroups: PropTypes.func.isRequired,
  addEntity: PropTypes.func.isRequired
};

export default withApi(ManualImportContainer);
