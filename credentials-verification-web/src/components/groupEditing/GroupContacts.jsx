import React from 'react';
import PropTypes from 'prop-types';
import ConnectionsTable from '../connections/Organisms/table/ConnectionsTable';
import SimpleLoading from '../common/Atoms/SimpleLoading/SimpleLoading';
import { getGroupContactColumns } from '../../helpers/tableDefinitions/contacts';
import { contactShape } from '../../helpers/propShapes';

const GroupContacts = ({
  contacts,
  loading,
  hasMore,
  selectedContacts,
  setSelectedContacts,
  onDeleteContact,
  handleContactsRequest
}) => {
  const handleDelete = val => {
    onDeleteContact([val]);
  };

  return (
    <div className="GroupContacts">
      {loading ? (
        <SimpleLoading />
      ) : (
        <ConnectionsTable
          contacts={contacts}
          selectedContacts={selectedContacts}
          setSelectedContacts={setSelectedContacts}
          handleContactsRequest={handleContactsRequest}
          columns={getGroupContactColumns(handleDelete)}
          hasMore={hasMore}
          size="md"
        />
      )}
    </div>
  );
};

GroupContacts.defaultProps = {
  loading: false,
  hasMore: false
};

GroupContacts.propTypes = {
  contacts: PropTypes.arrayOf(PropTypes.shape(contactShape)).isRequired,
  selectedContacts: PropTypes.arrayOf(PropTypes.string).isRequired,
  setSelectedContacts: PropTypes.func.isRequired,
  handleContactsRequest: PropTypes.func.isRequired,
  onDeleteContact: PropTypes.func.isRequired,
  loading: PropTypes.bool,
  hasMore: PropTypes.bool
};

export default GroupContacts;
