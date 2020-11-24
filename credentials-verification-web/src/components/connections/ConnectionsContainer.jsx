import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import Connections from './Connections';
import { withApi } from '../providers/withApi';
import { useContactsWithFilteredList } from '../../hooks/useContacts';

const ConnectionsContainer = ({ api }) => {
  const [loading, setLoading] = useState(false);
  const {
    contacts,
    filteredContacts,
    getContacts,
    handleContactsRequest,
    hasMore
  } = useContactsWithFilteredList(api.contactsManager, setLoading);

  const refreshContacts = () => getContacts({ pageSize: contacts.length, isRefresh: true });

  const inviteContact = contactId => api.contactsManager.generateConnectionToken(contactId);

  useEffect(() => {
    setLoading(true);
    if (!contacts.length) handleContactsRequest();
  }, []);

  // Wrapper to preserve 'this' context
  const getCredentials = connectionId => api.connector.getMessagesForConnection(connectionId);

  const tableProps = {
    contacts: filteredContacts,
    hasMore,
    getCredentials
  };

  return (
    <Connections
      tableProps={tableProps}
      handleContactsRequest={handleContactsRequest}
      inviteContact={inviteContact}
      refreshContacts={refreshContacts}
      loading={loading}
    />
  );
};

ConnectionsContainer.propTypes = {
  api: PropTypes.shape().isRequired
};

export default withApi(ConnectionsContainer);
