import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { withApi } from '../providers/withApi';
import GroupCreation from './GroupCreation';
import { withRedirector } from '../providers/withRedirector';
import { useContactStore, useContactUiState } from '../../hooks/useContactStore';
import { useGroupStore } from '../../hooks/useGroupStore';

const GroupCreationContainer = ({ redirector: { redirectToGroups } }) => {
  useContactUiState({ reset: true });
  useContactStore({ reset: true, fetch: true });
  const { createGroup, isSaving } = useGroupStore();

  const [groupName, setGroupName] = useState('');
  const [members, setMembers] = useState([]);
  const formRef = React.createRef();
  const formValues = { groupName };

  const handleCreateGroup = async () => {
    await createGroup({ name: groupName, members });
    redirectToGroups();
  };

  return (
    <GroupCreation
      createGroup={handleCreateGroup}
      formRef={formRef}
      updateForm={setGroupName}
      updateMembers={setMembers}
      formValues={formValues}
      isSaving={isSaving}
    />
  );
};

GroupCreationContainer.propTypes = {
  api: PropTypes.shape({
    groupsManager: PropTypes.shape({
      createGroup: PropTypes.func.isRequired,
      updateGroup: PropTypes.func.isRequired
    }).isRequired,
    contactsManager: PropTypes.shape({ getContacts: PropTypes.func.isRequired }).isRequired,
    wallet: PropTypes.shape({ signCredentials: PropTypes.func })
  }).isRequired,
  redirector: PropTypes.shape({ redirectToGroups: PropTypes.func.isRequired }).isRequired
};

export default withApi(withRedirector(GroupCreationContainer));
