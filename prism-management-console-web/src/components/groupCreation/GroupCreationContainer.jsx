import React, { useState } from 'react';
import PropTypes from 'prop-types';
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
  redirector: PropTypes.shape({ redirectToGroups: PropTypes.func.isRequired }).isRequired
};

export default withRedirector(GroupCreationContainer);
