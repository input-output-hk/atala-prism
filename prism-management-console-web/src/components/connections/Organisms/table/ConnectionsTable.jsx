import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { observer } from 'mobx-react-lite';
import { contactShape } from '../../../../helpers/propShapes';
import InfiniteScrollTable from '../../../common/Organisms/Tables/InfiniteScrollTable';
import { getContactColumns } from '../../../../helpers/tableDefinitions/contacts';
import EmptyComponent from '../../../common/Atoms/EmptyComponent/EmptyComponent';
import noContacts from '../../../../images/noConnections.svg';
import SimpleLoading from '../../../common/Atoms/SimpleLoading/SimpleLoading';
import { useContactStore, useContactUiState } from '../../../../hooks/useContactStore';
import { useSession } from '../../../../hooks/useSession';
import { CONFIRMED } from '../../../../helpers/constants';

import './_style.scss';

const ConnectionsTable = observer(
  ({
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
    const { isLoadingFirstPage, fetchMoreData, isFetching, hasMore } = useContactStore();
    const { displayedContacts, hasFiltersApplied, isSearching, isSorting } = useContactUiState();

    const emptyProps = {
      photoSrc: noContacts,
      model: t('groups.title'),
      isFilter: hasFiltersApplied,
      button: newContactButton
    };

    const renderEmpty = () => (
      <EmptyComponent {...emptyProps} button={accountStatus === CONFIRMED && newContactButton} />
    );

    const tableProps = {
      columns: columns || getContactColumns({ inviteContact, viewContactDetail }),
      data: displayedContacts,
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
      loading: isLoadingFirstPage || isSorting,
      fetchingMore: isFetching || isSearching,
      hasMore,
      renderEmpty
    };

    return isLoadingFirstPage ? <SimpleLoading /> : <InfiniteScrollTable {...tableProps} />;
  }
);

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
