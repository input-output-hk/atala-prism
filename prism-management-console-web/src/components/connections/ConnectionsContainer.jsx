import React from 'react';
import PropTypes from 'prop-types';
import Connections from './Connections';
import { withApi } from '../providers/withApi';
import { useContactsWithFilteredList } from '../../hooks/useContacts';

const ConnectionsContainer = ({ api }) => {
  const {
    contacts,
    filteredContacts,
    filterProps,
    getContacts,
    handleContactsRequest,
    hasMore,
    isLoading,
    isSearching
  } = useContactsWithFilteredList(api.contactsManager);

  const refreshContacts = () => getContacts({ pageSize: contacts.length, isRefresh: true });

  const inviteContact = contactId => api.contactsManager.generateConnectionToken(contactId);

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
      loading={isLoading}
      searching={isSearching}
      filterProps={filterProps}
    />
  );
};

ConnectionsContainer.propTypes = {
  api: PropTypes.shape({
    contactsManager: PropTypes.shape({ generateConnectionToken: PropTypes.func })
  }).isRequired
};

export default withApi(ConnectionsContainer);
