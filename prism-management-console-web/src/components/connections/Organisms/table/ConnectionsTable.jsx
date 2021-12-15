import React from 'react';
import PropTypes from 'prop-types';
import { observer } from 'mobx-react-lite';
import { useTranslation } from 'react-i18next';
import { contactShape } from '../../../../helpers/propShapes';
import InfiniteScrollTable from '../../../common/Organisms/Tables/InfiniteScrollTable';
import { getContactColumns } from '../../../../helpers/tableDefinitions/contacts';
import EmptyComponent from '../../../common/Atoms/EmptyComponent/EmptyComponent';
import noContacts from '../../../../images/noConnections.svg';
import { useSession } from '../../../../hooks/useSession';
import { CONFIRMED, CONTACT_ID_KEY } from '../../../../helpers/constants';
import CreateContactButton from '../../Atoms/ActionButtons/CreateContactButton';

import './_style.scss';

const ConnectionsTable = observer(
  ({
    contacts,
    fetchMoreData,
    hasMore,
    hasFiltersApplied,
    isLoading,
    isFetchingMore,
    columns,
    onSelect,
    selectedContactIds,
    inviteContact,
    viewContactDetail,
    shouldSelectRecipients
  }) => {
    const { t } = useTranslation();
    const { accountStatus } = useSession();

    const emptyProps = {
      photoSrc: noContacts,
      model: t('contacts.title'),
      isFilter: hasFiltersApplied
    };

    const renderEmpty = () => (
      <EmptyComponent
        {...emptyProps}
        button={accountStatus === CONFIRMED ? <CreateContactButton /> : undefined}
      />
    );

    const tableProps = {
      columns: columns || getContactColumns({ inviteContact, viewContactDetail }),
      data: contacts,
      selectionType: onSelect
        ? {
            selectedRowKeys: selectedContactIds,
            type: 'checkbox',
            onSelect,
            getCheckboxProps: () => ({
              disabled: !shouldSelectRecipients
            })
          }
        : undefined,
      rowKey: CONTACT_ID_KEY,
      getMoreData: fetchMoreData,
      loading: isLoading,
      fetchingMore: isFetchingMore,
      hasMore,
      renderEmpty
    };

    return <InfiniteScrollTable {...tableProps} />;
  }
);

ConnectionsTable.defaultProps = {
  contacts: [],
  columns: null,
  onSelect: null,
  selectedContactIds: [],
  inviteContact: null,
  viewContactDetail: null,
  shouldSelectRecipients: true
};

ConnectionsTable.propTypes = {
  contacts: PropTypes.arrayOf(contactShape),
  fetchMoreData: PropTypes.func.isRequired,
  hasMore: PropTypes.bool.isRequired,
  hasFiltersApplied: PropTypes.bool.isRequired,
  isLoading: PropTypes.bool.isRequired,
  isFetchingMore: PropTypes.bool.isRequired,
  columns: PropTypes.arrayOf(PropTypes.any),
  onSelect: PropTypes.func,
  selectedContactIds: PropTypes.arrayOf(PropTypes.string),
  inviteContact: PropTypes.func,
  viewContactDetail: PropTypes.func,
  shouldSelectRecipients: PropTypes.bool
};

export default ConnectionsTable;
