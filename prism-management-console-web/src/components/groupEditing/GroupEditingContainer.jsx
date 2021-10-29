import React, { useCallback, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useParams } from 'react-router-dom';
import PropTypes from 'prop-types';
import { message } from 'antd';
import GroupEditing from './GroupEditing';
import { withApi } from '../providers/withApi';
import Logger from '../../helpers/Logger';
import { useContacts } from '../../hooks/useContacts';

const GroupEditingContainer = ({ api }) => {
  const { t } = useTranslation();
  const { id } = useParams();

  const [loading, setLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [group, setGroup] = useState();

  const {
    contacts,
    filterProps,
    getMoreContacts,
    refreshContacts,
    hasMore,
    isLoading: loadingContacts
  } = useContacts(api.contactsManager, false);

  useEffect(() => {
    if (!group) {
      api.groupsManager
        .getAllGroups()
        .then(groups => {
          const result = groups.find(item => item.id === id);
          setGroup(result);
        })
        .catch(error => {
          Logger.error('[Groups.getGroups] Error while getting groups', error);
          message.error(t('errors.errorGetting', { model: 'Group' }));
        })
        .finally(() => setLoading(false));
    }
  }, [group, api.groupsManager, id, t]);

  const updateGroupContacts = useCallback(
    (refreshAllContacts = false) => {
      if (refreshAllContacts) refreshContacts();
      else getMoreContacts({ groupNameParam: group?.name });
    },
    [group, getMoreContacts, refreshContacts]
  );

  useEffect(() => {
    if (group && !contacts.length && hasMore) updateGroupContacts();
  }, [group, contacts, updateGroupContacts, hasMore]);

  const handleRemoveContacts = async contactIdsToRemove => {
    try {
      setIsSaving(true);
      await api.groupsManager.updateGroup(id, { contactIdsToRemove });
      updateGroupContacts(true);
    } catch (e) {
      message.error(t('groupEditing.errors.grpc'));
      Logger.error('groupEditing.errors.grpc', e);
    } finally {
      setIsSaving(false);
    }
  };

  const handleAddContacts = async contactIdsToAdd => {
    try {
      setIsSaving(true);
      await api.groupsManager.updateGroup(id, { contactIdsToAdd });
      updateGroupContacts(true);
    } catch (e) {
      message.error(t('groupEditing.errors.grpc'));
      Logger.error('groupEditing.errors.grpc', e);
    } finally {
      setIsSaving(false);
    }
  };

  const handleGroupRename = async newName => {
    try {
      setIsSaving(true);
      await api.groupsManager.updateGroup(id, { newName });
      updateGroupContacts(true);
    } catch (e) {
      message.error(t('groupEditing.errors.grpc'));
      Logger.error('groupEditing.errors.grpc', e);
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <GroupEditing
      group={group}
      filterProps={filterProps}
      contacts={contacts}
      handleContactsRequest={getMoreContacts}
      onGroupRename={handleGroupRename}
      onRemoveContacts={handleRemoveContacts}
      onAddContacts={handleAddContacts}
      loading={loading}
      loadingContacts={loadingContacts}
      isSaving={isSaving}
      hasMore={hasMore}
      fetchAllContacts={() => api.contactsManager.getAllContacts()}
    />
  );
};

GroupEditingContainer.defaultProps = {};

GroupEditingContainer.propTypes = {
  api: PropTypes.shape({
    contactsManager: PropTypes.shape({
      getContacts: PropTypes.func.isRequired,
      getAllContacts: PropTypes.func.isRequired
    }).isRequired,
    groupsManager: PropTypes.shape({
      getAllGroups: PropTypes.func.isRequired,
      updateGroup: PropTypes.func.isRequired
    }).isRequired
  }).isRequired
};

export default withApi(GroupEditingContainer);
