import React, { useState } from 'react';
import PropTypes from 'prop-types';
import i18n from 'i18next';
import CellRenderer from '../../../common/Atoms/CellRenderer/CellRenderer';
import StatusBadge from '../../Atoms/StatusBadge/StatusBadge';
import { dayMonthYearBackendFormatter } from '../../../../helpers/formatters';
import ActionButtons from '../../Atoms/ActionButtons/ActionButtons';
import holderDefaultAvatar from '../../../../images/holder-default-avatar.svg';
import { contactShape } from '../../../../helpers/propShapes';

import InfiniteScrollTable from '../../../common/Organisms/Tables/InfiniteScrollTable';
import PopOver from '../../../common/Organisms/Detail/PopOver';

import './_style.scss';

const translationKeyPrefix = 'contacts.table.columns';

const tp = chain => i18n.t(`${translationKeyPrefix}.${chain}`);

const getColumns = ({ inviteContact, viewContactDetail }) => {
  const generalColumns = [
    {
      key: 'avatar',
      width: 45,
      render: ({ avatar }) => (
        <img
          style={{ width: '40px', height: '40px' }}
          src={avatar || holderDefaultAvatar}
          alt="Avatar"
        />
      )
    },
    {
      key: 'contactName',
      render: ({ contactName }) => <CellRenderer title={tp('contactName')} value={contactName} />
    },
    {
      key: 'externalid',
      render: ({ externalid }) => <CellRenderer title={tp('externalid')} value={externalid} />
    },
    {
      key: 'creationDate',
      render: ({ creationDate }) => (
        <CellRenderer
          title={tp('creationDate')}
          value={dayMonthYearBackendFormatter(creationDate)}
        />
      )
    }
  ];

  const extraColumns = [
    {
      key: 'connectionstatus',
      render: ({ status }) => (
        <CellRenderer title={tp('contactStatus')}>
          <StatusBadge status={status} />
        </CellRenderer>
      )
    },
    {
      key: 'actions',
      render: contact => {
        const actionButtons = (
          <ActionButtons
            contact={contact}
            inviteContact={inviteContact}
            viewContactDetail={viewContactDetail}
          />
        );
        return <PopOver content={actionButtons} />;
      }
    }
  ];

  return viewContactDetail ? generalColumns.concat(extraColumns) : generalColumns;
};

const ConnectionsTable = ({
  contacts,
  setSelectedContacts,
  selectedContacts,
  inviteContact,
  handleContactsRequest,
  hasMore,
  viewContactDetail
}) => {
  const [loading, setLoading] = useState(false);

  const getMoreData = () => {
    setLoading(true);
    return handleContactsRequest().finally(() => setLoading(false));
  };
  return (
    <div className="ConnectionsTable">
      <InfiniteScrollTable
        columns={getColumns({ inviteContact, viewContactDetail })}
        data={contacts}
        loading={loading}
        getMoreData={getMoreData}
        hasMore={hasMore}
        rowKey="contactid"
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
  setSelectedContacts: null,
  selectedContacts: [],
  inviteContact: null,
  viewContactDetail: null
};

ConnectionsTable.propTypes = {
  contacts: PropTypes.arrayOf(PropTypes.shape(contactShape)),
  setSelectedContacts: PropTypes.func,
  selectedContacts: PropTypes.arrayOf(PropTypes.string),
  inviteContact: PropTypes.func,
  viewContactDetail: PropTypes.func,
  handleContactsRequest: PropTypes.func.isRequired,
  hasMore: PropTypes.bool.isRequired
};

export default ConnectionsTable;
