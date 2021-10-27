import React from 'react';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { observer } from 'mobx-react-lite';
import Logger from '../../helpers/Logger';
import Groups from './Groups';
import { withApi } from '../providers/withApi';
import { useGroupStore, useGroupUiState } from '../../hooks/useGroupStore';

const GroupsContainer = observer(({ api }) => {
  useGroupStore({ fetch: true, reset: true });
  useGroupUiState({ reset: true });

  const { t } = useTranslation();

  const handleGroupDeletion = group =>
    api.groupsManager
      .deleteGroup(group.id)
      .then(() => {
        message.success(t('groups.deletionSuccess', { groupName: group.name }));
      })
      .catch(error => {
        Logger.error('[GroupsContainer.handleGroupDeletion] Error: ', error);
        message.error(t('errors.errorDeletingGroup', { groupName: group.name }));
      });

  const copyGroup = ({ numberOfContacts, name: groupName }, copyName) =>
    api.groupsManager
      .createGroup(copyName)
      .then(({ id }) =>
        api.contactsManager
          .getContacts({ limit: numberOfContacts, groupName })
          .then(({ contactsList }) =>
            api.groupsManager.updateGroup(id, {
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

GroupsContainer.propTypes = {
  api: PropTypes.shape().isRequired
};

export default withApi(GroupsContainer);
