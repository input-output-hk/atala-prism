import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { contactShape } from '../../../../helpers/propShapes';

import InfiniteScrollTable from '../../../common/Organisms/Tables/InfiniteScrollTable';
import { getContactColumns } from '../../../../helpers/tableDefinitions/contacts';

import './_style.scss';
import { TABLE_HEIGHTS } from '../../../../helpers/constants';

const ConnectionsTable = ({
  contacts,
  setSelectedContacts,
  selectedContacts,
  inviteContact,
  handleContactsRequest,
  hasMore,
  viewContactDetail,
  size
}) => {
  const [loading, setLoading] = useState(false);

  const getMoreData = () => {
    setLoading(true);
    return handleContactsRequest().finally(() => setLoading(false));
  };

  return (
    <div className="ConnectionsTable">
      <InfiniteScrollTable
        columns={getContactColumns({ inviteContact, viewContactDetail })}
        data={contacts}
        loading={loading}
        getMoreData={getMoreData}
        hasMore={hasMore}
        rowKey="contactid"
        size={size}
        selectionType={
          setSelectedContacts && {
            selectedRowKeys: selectedContacts,
            type: 'checkbox',
            onChange: setSelectedContacts
          }
        }
      />
    </div>
  );
};

ConnectionsTable.defaultProps = {
  contacts: [],
  viewContactDetail: null,
  setSelectedContacts: null,
  selectedContacts: [],
  inviteContact: null,
  size: 'xl'
};

ConnectionsTable.propTypes = {
  contacts: PropTypes.arrayOf(PropTypes.shape(contactShape)),
  setSelectedContacts: PropTypes.func,
  selectedContacts: PropTypes.arrayOf(PropTypes.string),
  inviteContact: PropTypes.func,
  viewContactDetail: PropTypes.func,
  handleContactsRequest: PropTypes.func.isRequired,
  hasMore: PropTypes.bool.isRequired,
  size: PropTypes.oneOf(Object.keys(TABLE_HEIGHTS))
};

export default ConnectionsTable;
