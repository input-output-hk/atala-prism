import React from 'react';
import { useParams } from 'react-router-dom';
import { observer } from 'mobx-react-lite';
import GroupEditing from './GroupEditing';
import { useCurrentGroupState } from '../../hooks/useCurrentGroupState';

const GroupEditingContainer = observer(() => {
  const { id } = useParams();
  const { updateGroupName, updateGroupMembers } = useCurrentGroupState(id);

  const handleRemoveContacts = async contactIdsToRemove =>
    updateGroupMembers({ contactIdsToRemove });

  const handleAddContacts = async contactIdsToAdd => updateGroupMembers({ contactIdsToAdd });

  const handleGroupRename = async newName => updateGroupName(newName);

  return (
    <GroupEditing
      onGroupRename={handleGroupRename}
      onRemoveContacts={handleRemoveContacts}
      onAddContacts={handleAddContacts}
    />
  );
});

export default GroupEditingContainer;
