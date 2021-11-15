import React, { useEffect } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { observer } from 'mobx-react-lite';
import InfiniteScrollTable from '../../../common/Organisms/Tables/InfiniteScrollTable';
import { getGroupColumns } from '../../../../helpers/tableDefinitions/groups';
import { useGroupStore, useGroupUiState } from '../../../../hooks/useGroupStore';
import noGroups from '../../../../images/noGroups.svg';
import EmptyComponent from '../../../common/Atoms/EmptyComponent/EmptyComponent';
import { useSession } from '../../../../hooks/useSession';
import { CONFIRMED } from '../../../../helpers/constants';

import './_style.scss';

const GroupsTable = observer(
  ({
    onCopy,
    setGroupToDelete,
    newGroupButton,
    selectedGroups,
    setSelectedGroups,
    shouldSelectRecipients
  }) => {
    const { t } = useTranslation();
    const { accountStatus } = useSession();
    const { groups, fetchMoreData, isLoadingFirstPage, isFetching, hasMore } = useGroupStore();
    const { hasFiltersApplied, isSearching, isSorting } = useGroupUiState();

    useEffect(() => {
      fetchMoreData();
    }, [fetchMoreData]);

    const emptyProps = {
      photoSrc: noGroups,
      model: t('groups.title'),
      isFilter: hasFiltersApplied,
      button: newGroupButton
    };

    const renderEmpty = () => (
      <EmptyComponent {...emptyProps} button={accountStatus === CONFIRMED && newGroupButton} />
    );

    const tableProps = {
      columns: getGroupColumns({ onCopy, setGroupToDelete, setSelectedGroups }),
      data: groups,
      selectionType: !setSelectedGroups
        ? null
        : {
            selectedRowKeys: selectedGroups,
            type: 'checkbox',
            onChange: setSelectedGroups,
            getCheckboxProps: () => ({
              disabled: !shouldSelectRecipients
            })
          },
      rowKey: 'name',
      getMoreData: fetchMoreData,
      loading: isSearching || isSorting,
      fetchingMore: isFetching,
      hasMore,
      renderEmpty
    };

    return <InfiniteScrollTable {...tableProps} emptyProps={emptyProps} />;
  }
);

GroupsTable.defaultProps = {
  groups: [],
  selectedGroups: [],
  setSelectedGroups: null,
  setGroupToDelete: null,
  shouldSelectRecipients: false
};

GroupsTable.propTypes = {
  setGroupToDelete: PropTypes.func,
  onCopy: PropTypes.func.isRequired,
  groups: PropTypes.arrayOf(PropTypes.object),
  onPageChange: PropTypes.func.isRequired,
  selectedGroups: PropTypes.arrayOf(PropTypes.string),
  setSelectedGroups: PropTypes.func,
  hasMore: PropTypes.bool.isRequired,
  searching: PropTypes.bool.isRequired,
  shouldSelectRecipients: PropTypes.bool,
  getMoreGroups: PropTypes.func.isRequired
};

export default GroupsTable;
