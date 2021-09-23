import React, { useCallback, useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { contactShape } from '../../../../helpers/propShapes';
import InfiniteScrollTable from '../../../common/Organisms/Tables/InfiniteScrollTable';
import { getContactColumns } from '../../../../helpers/tableDefinitions/contacts';
import { useScrolledToBottom } from '../../../../hooks/useScrolledToBottom';

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
  shouldSelectRecipients,
  searchDueGeneralScroll
}) => {
  const [loading, setLoading] = useState(false);
  const { timesScrolledToBottom } = useScrolledToBottom(hasMore, loading, 'ConnectionsTable ');
  const [lastUpdated, setLastUpdated] = useState(timesScrolledToBottom);

  // leave this trigger for backward compatibility, when all tables uses useScrolledToBottom remove searchDueGeneralScroll
  const handleGetMoreData = () => !searchDueGeneralScroll && getMoreData();

  const getMoreData = useCallback(() => {
    if (loading) return;
    setLoading(true);
    handleContactsRequest({ onFinish: () => setLoading(false) });
  }, [loading, handleContactsRequest]);

  useEffect(() => {
    if (timesScrolledToBottom !== lastUpdated && searchDueGeneralScroll) {
      setLastUpdated(timesScrolledToBottom);
      getMoreData();
    }
  }, [timesScrolledToBottom, lastUpdated, searchDueGeneralScroll, getMoreData]);

  return (
    <div className="ConnectionsTable InfiniteScrollTableContainer">
      <InfiniteScrollTable
        columns={columns || getContactColumns({ inviteContact, viewContactDetail })}
        data={contacts}
        loading={loading}
        getMoreData={handleGetMoreData}
        hasMore={hasMore}
        rowKey="contactId"
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
  shouldSelectRecipients: true,
  searchDueGeneralScroll: false
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
  shouldSelectRecipients: PropTypes.bool,
  searchDueGeneralScroll: PropTypes.bool
};

export default ConnectionsTable;
