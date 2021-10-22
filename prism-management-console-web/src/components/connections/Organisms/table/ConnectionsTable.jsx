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
    overrideContacts,
    contacts,
    overrideLoading,
    loading,
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
    const { displayedContacts, hasFiltersApplied, isSearching, isSorting } = useContactUiState();
    const { isLoadingFirstPage, fetchMoreData, isFetching, hasMore } = useContactStore();

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
      data: overrideContacts ? contacts : displayedContacts,
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
      loading: overrideLoading ? loading : isLoadingFirstPage || isSorting,
      fetchingMore: isFetching || isSearching,
      hasMore,
      renderEmpty
    };

    return isLoadingFirstPage && !overrideLoading ? (
      <SimpleLoading />
    ) : (
      <InfiniteScrollTable {...tableProps} />
    );
  }
);

ConnectionsTable.defaultProps = {
  overrideContacts: false,
  overrideLoading: false,
  shouldSelectRecipients: true
};

ConnectionsTable.propTypes = {
  overrideContacts: PropTypes.bool,
  contacts: PropTypes.arrayOf(contactShape),
  overrideLoading: PropTypes.bool,
  loading: PropTypes.bool,
  columns: PropTypes.arrayOf(PropTypes.any),
  setSelectedContacts: PropTypes.func,
  selectedContacts: PropTypes.arrayOf(PropTypes.string),
  inviteContact: PropTypes.func,
  viewContactDetail: PropTypes.func,
  shouldSelectRecipients: PropTypes.bool,
  newContactButton: PropTypes.bool
};

export default ConnectionsTable;
