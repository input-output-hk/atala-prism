import React, { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useParams } from 'react-router-dom';
import PropTypes from 'prop-types';
import { message } from 'antd';
import GroupEditing from './GroupEditing';
import { withApi } from '../providers/withApi';
import Logger from '../../helpers/Logger';
import { useContactsWithFilteredList } from '../../hooks/useContacts';

const GroupEditingContainer = ({ api }) => {
  const { t } = useTranslation();
  const { id } = useParams();

  const [loading, setLoading] = useState(true);
  const [loadingContacts, setLoadingContacts] = useState(false);
  const [searching, setSearching] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [group, setGroup] = useState();

  const {
    contacts,
    filteredContacts,
    filterProps,
    handleContactsRequest,
    hasMore
  } = useContactsWithFilteredList(api.contactsManager, setLoadingContacts, setSearching);

  useEffect(() => {
    getGroup();
  }, []);

  useEffect(() => {
    if (!contacts.length) getGroupContacts();
  }, [group]);

  const getGroup = async () => {
    try {
      setLoading(true);
      const groups = await api.groupsManager.getGroups();
      const result = groups.find(item => item.id === id);
      setGroup(result);
    } catch (error) {
      Logger.error('[Groups.getGroups] Error while getting groups', error);
      message.error(t('errors.errorGetting', { model: 'Group' }));
    } finally {
      setLoading(false);
    }
  };

  const getGroupContacts = (refreshAllContacts = false) => {
    if (group?.name) {
      setLoadingContacts(true);
      handleContactsRequest(group.name, refreshAllContacts);
    }
  };

  const handleRemoveContacts = async contactsToRemove => {
    try {
      setIsSaving(true);
      await api.groupsManager.updateGroup(id, [], contactsToRemove);
      getGroupContacts(true);
    } catch (e) {
      message.error(t('groupEditing.errors.grpc'));
      Logger.error('groupEditing.errors.grpc', e);
    } finally {
      setIsSaving(false);
    }
  };

  const handleAddContacts = async aContactsList => {
    try {
      setIsSaving(true);
      await api.groupsManager.updateGroup(id, aContactsList);
      getGroupContacts(true);
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
      contacts={filteredContacts}
      handleContactsRequest={handleContactsRequest}
      onRemoveContacts={handleRemoveContacts}
      onAddContacts={handleAddContacts}
      loading={loading}
      loadingContacts={loadingContacts}
      isSaving={isSaving}
      hasMore={hasMore}
    />
  );
};

GroupEditingContainer.defaultProps = {};

GroupEditingContainer.propTypes = {
  api: PropTypes.shape({
    contactsManager: PropTypes.shape({
      getContacts: PropTypes.func
    }),
    groupsManager: PropTypes.shape({
      getGroups: PropTypes.func,
      updateGroup: PropTypes.func
    })
  }).isRequired
};

export default withApi(GroupEditingContainer);
