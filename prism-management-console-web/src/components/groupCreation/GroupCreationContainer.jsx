import React, { createRef, useState } from 'react';
import GroupCreation from './GroupCreation';
import { useContactStore, useContactUiState } from '../../hooks/useContactStore';
import { useGroupStore } from '../../hooks/useGroupStore';
import { useRedirector } from '../../hooks/useRedirector';

const GroupCreationContainer = () => {
  const { redirectToGroups } = useRedirector();
  const { contactUiState } = useContactStore({ reset: true });
  useContactUiState({ instance: contactUiState, reset: true });
  const { createGroup, isSaving } = useGroupStore();

  const [groupName, setGroupName] = useState('');
  const [members, setMembers] = useState([]);
  const formRef = createRef();
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

export default GroupCreationContainer;
