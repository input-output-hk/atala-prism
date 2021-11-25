import React, { createRef, useState } from 'react';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import { useRedirector } from '../../hooks/useRedirector';
import { useGroupsPageStore } from '../../hooks/useGroupsPageStore';
import GroupCreation from './GroupCreation';

const GroupCreationContainer = () => {
  const { redirectToGroups } = useRedirector();
  const { t } = useTranslation();

  const { createGroup, isSaving } = useGroupsPageStore();

  const [groupName, setGroupName] = useState('');
  const [members, setMembers] = useState([]);
  const formRef = createRef();
  const formValues = { groupName };

  const handleCreateGroup = () => {
    createGroup({
      name: groupName,
      members,
      onSuccess: () => {
        redirectToGroups();
        message.success(t('groupCreation.success', { groupName }));
      },
      onError: () => {
        message.error(t('groupCreation.errors.grpc', { groupName }));
      }
    });
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
