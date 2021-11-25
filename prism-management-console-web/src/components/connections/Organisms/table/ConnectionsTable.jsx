import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { contactShape } from '../../../../helpers/propShapes';
import InfiniteScrollTable from '../../../common/Organisms/Tables/InfiniteScrollTable';
import { getContactColumns } from '../../../../helpers/tableDefinitions/contacts';
import EmptyComponent from '../../../common/Atoms/EmptyComponent/EmptyComponent';
import noContacts from '../../../../images/noConnections.svg';
import { useSession } from '../../../../hooks/useSession';
import { CONFIRMED } from '../../../../helpers/constants';

import './_style.scss';

const ConnectionsTable = ({
  contacts,
  fetchMoreData,
  hasMore,
  hasFiltersApplied,
  isLoading,
  isFetchingMore,
  columns,
  setSelectedContacts,
  selectedContacts,
  inviteContact,
  viewContactDetail,
  shouldSelectRecipients,
  newContactButton
}) => {
  const { t } = useTranslation();
  const { accountStatus } = useSession();

  const emptyProps = {
    photoSrc: noContacts,
    model: t('contacts.title'),
    isFilter: hasFiltersApplied,
    button: newContactButton
  };

  const renderEmpty = () => (
    <EmptyComponent {...emptyProps} button={accountStatus === CONFIRMED && newContactButton} />
  );

  const tableProps = {
    columns: columns || getContactColumns({ inviteContact, viewContactDetail }),
    data: contacts,
    selectionType: setSelectedContacts && {
      selectedRowKeys: selectedContacts,
      type: 'checkbox',
      onChange: setSelectedContacts,
      getCheckboxProps: () => ({
        disabled: !shouldSelectRecipients
      })
    },
    rowKey: 'contactId',
    getMoreData: fetchMoreData,
    loading: isLoading,
    fetchingMore: isFetchingMore,
    hasMore,
    renderEmpty
  };

  return <InfiniteScrollTable {...tableProps} />;
};
ConnectionsTable.defaultProps = {
  contacts: [],
  columns: null,
  setSelectedContacts: null,
  selectedContacts: [],
  inviteContact: null,
  viewContactDetail: null,
  shouldSelectRecipients: true,
  newContactButton: null
};

ConnectionsTable.propTypes = {
  contacts: PropTypes.arrayOf(contactShape),
  fetchMoreData: PropTypes.func.isRequired,
  hasMore: PropTypes.bool.isRequired,
  hasFiltersApplied: PropTypes.bool.isRequired,
  isLoading: PropTypes.bool.isRequired,
  isFetchingMore: PropTypes.bool.isRequired,
  columns: PropTypes.arrayOf(PropTypes.any),
  setSelectedContacts: PropTypes.func,
  selectedContacts: PropTypes.arrayOf(PropTypes.string),
  inviteContact: PropTypes.func,
  viewContactDetail: PropTypes.func,
  shouldSelectRecipients: PropTypes.bool,
  newContactButton: PropTypes.node
};

export default ConnectionsTable;
