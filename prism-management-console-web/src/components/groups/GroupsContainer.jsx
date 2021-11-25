import React, { useEffect } from 'react';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import { observer } from 'mobx-react-lite';
import Groups from './Groups';
import { useGroupsPageStore } from '../../hooks/useGroupsPageStore';

const GroupsContainer = observer(() => {
  const groupsPageStore = useGroupsPageStore();
  const { init } = groupsPageStore;

  const { t } = useTranslation();

  useEffect(() => {
    init();
  }, [init]);

  const handleGroupDeletion = group =>
    groupsPageStore.deleteGroup({
      groupId: group.id,
      onError: () => {
        message.error(t('errors.errorDeletingGroup', { groupName: group.name }));
      },
      onSuccess: () => {
        message.success(t('groups.deletionSuccess', { groupName: group.name }));
      }
    });

  const copyGroup = ({ numberOfContacts, name: groupName }, copyName) =>
    groupsPageStore.copyGroup({
      groupName,
      copyName,
      numberOfContacts,
      onError: () => {
        message.error(t('groups.copy.error'));
      },
      onSuccess: () => {
        message.success(t('groups.copy.success'));
      }
    });

  return <Groups copyGroup={copyGroup} handleGroupDeletion={handleGroupDeletion} />;
});

export default GroupsContainer;
