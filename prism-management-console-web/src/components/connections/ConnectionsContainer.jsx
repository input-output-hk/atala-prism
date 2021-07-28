import React from 'react';
import PropTypes from 'prop-types';
import Connections from './Connections';
import { withApi } from '../providers/withApi';
import { useContacts } from '../../hooks/useContacts';

const ConnectionsContainer = ({ api }) => {
  const {
    contacts,
    refreshContacts,
    getMoreContacts,
    hasMore,
    isLoading,
    isSearching,
    filterProps,
    sortProps
  } = useContacts(api.contactsManager);

  const tableProps = {
    contacts,
    hasMore
  };

  return (
    <Connections
      tableProps={tableProps}
      handleContactsRequest={getMoreContacts}
      refreshContacts={refreshContacts}
      loading={isLoading}
      searching={isSearching}
      filterProps={filterProps}
      sortProps={sortProps}
    />
  );
};

ConnectionsContainer.propTypes = {
  api: PropTypes.shape({
    contactsManager: PropTypes.shape({ getContacts: PropTypes.func })
  }).isRequired
};

export default withApi(ConnectionsContainer);
