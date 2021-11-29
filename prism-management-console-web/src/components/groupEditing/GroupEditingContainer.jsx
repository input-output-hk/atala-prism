import React, { useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { observer } from 'mobx-react-lite';
import { message } from 'antd';
import i18n from 'i18next';
import { useCurrentGroupStore } from '../../hooks/useGroupStore';
import GroupEditing from './GroupEditing';

const GroupEditingContainer = observer(() => {
  const { id } = useParams();
  const { updateGroupName, updateGroupMembers, init } = useCurrentGroupStore();

  useEffect(() => {
    if (id) init(id);
  }, [id, init]);

  const handleRemoveContacts = contactIdsToRemove =>
    updateGroupMembers({
      membersUpdate: { contactIdsToRemove },
      onSuccess: () => {
        message.success(i18n.t('groupEditing.success'));
      },
      onError: () => {
        message.error(i18n.t('groupEditing.errors.grpc'));
      }
    });

  const handleAddContacts = contactIdsToAdd =>
    updateGroupMembers({
      membersUpdate: { contactIdsToAdd },
      onSuccess: () => {
        message.success(i18n.t('groupEditing.success'));
      },
      onError: () => {
        message.error(i18n.t('groupEditing.errors.grpc'));
      }
    });

  const handleGroupRename = newName =>
    updateGroupName({
      newName,
      onSuccess: () => {
        message.success(i18n.t('groupEditing.success'));
      },
      onError: () => {
        message.error(i18n.t('groupEditing.errors.grpc'));
      }
    });

  return (
    <GroupEditing
      onGroupRename={handleGroupRename}
      onRemoveContacts={handleRemoveContacts}
      onAddContacts={handleAddContacts}
    />
  );
});

export default GroupEditingContainer;
