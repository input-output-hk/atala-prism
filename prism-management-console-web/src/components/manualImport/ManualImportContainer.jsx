import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import ManualImport from './ManualImport';
import Logger from '../../helpers/Logger';
import { credentialShape, credentialTypeShape, groupShape } from '../../helpers/propShapes';
import { useApi } from '../../hooks/useApi';

const ManualImportContainer = ({
  useCaseProps,
  credentialType,
  recipients,
  selectedGroups,
  setSelectedGroups,
  addEntity
}) => {
  const { t } = useTranslation();
  const { groupsManager } = useApi();
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
  useCaseProps: PropTypes.shape({
    showGroupSelection: PropTypes.bool
  }).isRequired,
  credentialType: credentialTypeShape,
  hasSelectedRecipients: PropTypes.bool.isRequired,
  setContacts: PropTypes.func.isRequired,
  recipients: PropTypes.arrayOf(credentialShape),
  selectedGroups: PropTypes.arrayOf(PropTypes.shape(groupShape)).isRequired,
  setSelectedGroups: PropTypes.func.isRequired,
  addEntity: PropTypes.func.isRequired
};

export default ManualImportContainer;
