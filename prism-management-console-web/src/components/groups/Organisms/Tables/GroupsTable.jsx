import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { observer } from 'mobx-react-lite';
import InfiniteScrollTable from '../../../common/Organisms/Tables/InfiniteScrollTable';
import { getGroupColumns } from '../../../../helpers/tableDefinitions/groups';
import noGroups from '../../../../images/noGroups.svg';
import EmptyComponent from '../../../common/Atoms/EmptyComponent/EmptyComponent';
import { useSession } from '../../../../hooks/useSession';
import { CONFIRMED, GROUP_ID_KEY } from '../../../../helpers/constants';
import { groupShape } from '../../../../helpers/propShapes';

import './_style.scss';

const GroupsTable = observer(
  ({
    groups,
    fetchMoreData,
    isFetchingMore,
    hasMore,
    hasFiltersApplied,
    isLoading,
    onCopy,
    setGroupToDelete,
    newGroupButton,
    selectedGroups,
    onSelect,
    shouldSelectRecipients
  }) => {
    const { t } = useTranslation();
    const { accountStatus } = useSession();

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
      columns: getGroupColumns({ onCopy, setGroupToDelete, setGroup: !!onSelect }),
      data: groups,
      selectionType: onSelect
        ? {
            selectedRowKeys: selectedGroups,
            type: 'checkbox',
            onSelect,
            getCheckboxProps: () => ({
              disabled: !shouldSelectRecipients
            })
          }
        : undefined,
      rowKey: GROUP_ID_KEY,
      getMoreData: fetchMoreData,
      loading: isLoading,
      fetchingMore: isFetchingMore,
      hasMore,
      renderEmpty
    };

    return <InfiniteScrollTable {...tableProps} emptyProps={emptyProps} />;
  }
);

GroupsTable.defaultProps = {
  groups: [],
  selectedGroups: [],
  onSelect: null,
  setGroupToDelete: null,
  shouldSelectRecipients: false
};

GroupsTable.propTypes = {
  onCopy: PropTypes.func,
  setGroupToDelete: PropTypes.func,
  newGroupButton: PropTypes.node,
  selectedGroups: PropTypes.arrayOf(PropTypes.string),
  onSelect: PropTypes.func,
  shouldSelectRecipients: PropTypes.bool,
  groups: PropTypes.arrayOf(groupShape),
  fetchMoreData: PropTypes.func.isRequired,
  isFetchingMore: PropTypes.bool.isRequired,
  hasMore: PropTypes.bool.isRequired,
  hasFiltersApplied: PropTypes.bool.isRequired,
  isLoading: PropTypes.bool.isRequired
};

export default GroupsTable;
