import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import Connections from './Connections';
import { withApi } from '../providers/withApi';
import { useContactsWithFilteredList } from '../../hooks/useContacts';

const ConnectionsContainer = ({ api }) => {
  const [loading, setLoading] = useState(true);
  const [searching, setSearching] = useState(true);
  const {
    contacts,
    filteredContacts,
    filterProps,
    getContacts,
    handleContactsRequest,
    hasMore
  } = useContactsWithFilteredList(api.contactsManager, setLoading, setSearching);

  const refreshContacts = () => getContacts({ pageSize: contacts.length, isRefresh: true });

  const inviteContact = contactId => api.contactsManager.generateConnectionToken(contactId);

  useEffect(() => {
    if (!contacts.length) handleContactsRequest();
  }, []);

  const tableProps = {
    contacts: filteredContacts,
    hasMore
  };

  return (
    <Connections
      tableProps={tableProps}
      handleContactsRequest={handleContactsRequest}
      inviteContact={inviteContact}
      refreshContacts={refreshContacts}
      loading={loading}
      searching={searching}
      filterProps={filterProps}
    />
  );
};

ConnectionsContainer.propTypes = {
  api: PropTypes.shape().isRequired
};

export default withApi(ConnectionsContainer);
