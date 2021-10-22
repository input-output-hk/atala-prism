import React from 'react';
import PropTypes from 'prop-types';
import ConnectionsTable from '../connections/Organisms/table/ConnectionsTable';
import { getGroupContactColumns } from '../../helpers/tableDefinitions/contacts';
import { contactShape } from '../../helpers/propShapes';

const GroupContacts = ({
  groupName,
  selectedContacts,
  setSelectedContacts,
  onDeleteContact,
  handleContactsRequest
}) => {
  const handleDelete = val => onDeleteContact([val]);

  return (
    <div className="GroupContacts ">
      <div className="InfiniteScrollTableContainer">
        <ConnectionsTable
          groupName={groupName}
          columns={getGroupContactColumns(handleDelete)}
          selectedContacts={selectedContacts}
          setSelectedContacts={setSelectedContacts}
          handleContactsRequest={handleContactsRequest}
          size="md"
          searchDueGeneralScroll
        />
      </div>
    </div>
  );
};

GroupContacts.propTypes = {
  selectedContacts: PropTypes.arrayOf(PropTypes.string).isRequired,
  setSelectedContacts: PropTypes.func.isRequired,
  handleContactsRequest: PropTypes.func.isRequired,
  onDeleteContact: PropTypes.func.isRequired
};

export default GroupContacts;
