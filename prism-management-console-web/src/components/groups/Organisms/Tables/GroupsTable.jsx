import React from 'react';
import PropTypes from 'prop-types';
import { observer } from 'mobx-react-lite';
import InfiniteScrollTable from '../../../common/Organisms/Tables/InfiniteScrollTable';
import { getGroupColumns } from '../../../../helpers/tableDefinitions/groups';
import { useGroupStore, useGroupUiState } from '../../../../hooks/useGroupStore';

import './_style.scss';

const GroupsTable = observer(
  ({
    setGroupToDelete,
    selectedGroups,
    setSelectedGroups,
    onCopy,
    shouldSelectRecipients,
    hasMore
  }) => {
    const { groups, fetchGroupsNextPage, fetchSearchResultsNextPage, isLoading } = useGroupStore();
    const { hasFiltersApplied, filteredGroups, isSearching } = useGroupUiState();

    const getDataSource = hasFiltersApplied ? filteredGroups : groups;

    const tableProps = {
      columns: getGroupColumns({ onCopy, setGroupToDelete, setSelectedGroups }),
      data: getDataSource,
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
      hasMore,
      getMoreData: hasFiltersApplied ? fetchSearchResultsNextPage : fetchGroupsNextPage,
      searching: isSearching,
      loading: isLoading
    };

    return <InfiniteScrollTable {...tableProps} />;
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
