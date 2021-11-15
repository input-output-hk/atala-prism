import React from 'react';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import { observer } from 'mobx-react-lite';
import Logger from '../../helpers/Logger';
import Groups from './Groups';
import { useGroupStore, useGroupUiState } from '../../hooks/useGroupStore';
import { useApi } from '../../hooks/useApi';

const GroupsContainer = observer(() => {
  const { groupsManager, contactsManager } = useApi();
  useGroupStore({ reset: true });
  useGroupUiState({ reset: true });

  const { t } = useTranslation();

  const handleGroupDeletion = group =>
    groupsManager
      .deleteGroup(group.id)
      .then(() => {
        message.success(t('groups.deletionSuccess', { groupName: group.name }));
      })
      .catch(error => {
        Logger.error('[GroupsContainer.handleGroupDeletion] Error: ', error);
        message.error(t('errors.errorDeletingGroup', { groupName: group.name }));
      });

  const copyGroup = ({ numberOfContacts, name: groupName }, copyName) =>
    groupsManager
      .createGroup(copyName)
      .then(({ id }) =>
        contactsManager
          .getContacts({ limit: numberOfContacts, groupName })
          .then(({ contactsList }) =>
            groupsManager.updateGroup(id, {
              contactIdsToAdd: contactsList.map(({ contactId }) => contactId)
            })
          )
      )
      .then(() => {
        message.success(t('groups.copy.success'));
      })
      .catch(error => {
        Logger.error('[GroupsContainer.copyGroup] Error: ', error);
        message.error(t('errors.errorCopyingGroup'));
      });

  return <Groups copyGroup={copyGroup} handleGroupDeletion={handleGroupDeletion} />;
});

export default GroupsContainer;
