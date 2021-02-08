import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { contactShape } from '../../../../helpers/propShapes';

import InfiniteScrollTable from '../../../common/Organisms/Tables/InfiniteScrollTable';
import { getContactColumns } from '../../../../helpers/tableDefinitions/contacts';

import './_style.scss';

const ConnectionsTable = ({
  contacts,
  setSelectedContacts,
  selectedContacts,
  inviteContact,
  handleContactsRequest,
  hasMore,
  viewContactDetail,
  columns,
  searching,
  shouldSelectRecipients
}) => {
  const [loading, setLoading] = useState(false);

  const getMoreData = () => {
    setLoading(true);
    return handleContactsRequest().finally(() => setLoading(false));
  };

  return (
    <div className="ConnectionsTable">
      <InfiniteScrollTable
        columns={columns || getContactColumns({ inviteContact, viewContactDetail })}
        data={contacts}
        loading={loading}
        getMoreData={getMoreData}
        hasMore={hasMore}
        rowKey="contactid"
        searching={searching}
        selectionType={
          setSelectedContacts && {
            selectedRowKeys: selectedContacts,
            type: 'checkbox',
            onChange: setSelectedContacts,
            getCheckboxProps: () => ({
              disabled: !shouldSelectRecipients
            })
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
  searching: false,
  columns: undefined,
  handleContactsRequest: null,
  shouldSelectRecipients: true
};

ConnectionsTable.propTypes = {
  contacts: PropTypes.arrayOf(PropTypes.shape(contactShape)),
  setSelectedContacts: PropTypes.func,
  selectedContacts: PropTypes.arrayOf(PropTypes.string),
  inviteContact: PropTypes.func,
  viewContactDetail: PropTypes.func,
  handleContactsRequest: PropTypes.func,
  hasMore: PropTypes.bool.isRequired,
  searching: PropTypes.bool,
  columns: PropTypes.arrayOf(PropTypes.any),
  shouldSelectRecipients: PropTypes.bool
};

export default ConnectionsTable;
