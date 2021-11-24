import React, { createRef, useState } from 'react';
import GroupCreation from './GroupCreation';
import { useRedirector } from '../../hooks/useRedirector';
import { useGroupsPageStore } from '../../hooks/useGroupsPageStore';

const GroupCreationContainer = () => {
  const { redirectToGroups } = useRedirector();

  const { createGroup, isSaving } = useGroupsPageStore();

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
