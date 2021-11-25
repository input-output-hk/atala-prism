import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { observer } from 'mobx-react-lite';
import InfiniteScrollTable from '../../../common/Organisms/Tables/InfiniteScrollTable';
import { getGroupColumns } from '../../../../helpers/tableDefinitions/groups';
import noGroups from '../../../../images/noGroups.svg';
import EmptyComponent from '../../../common/Atoms/EmptyComponent/EmptyComponent';
import { useSession } from '../../../../hooks/useSession';
import { CONFIRMED } from '../../../../helpers/constants';

import './_style.scss';
import { useGroupsPageStore } from '../../../../hooks/useGroupsPageStore';

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
    const {
      groups,
      fetchMoreData,
      isFetching,
      hasMore,
      hasFiltersApplied,
      isSearching,
      isSaving
    } = useGroupsPageStore();

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
      loading: isSearching || isSaving,
      fetchingMore: isFetching,
      hasMore,
      renderEmpty
    };

    return <InfiniteScrollTable {...tableProps} emptyProps={emptyProps} />;
  }
);

GroupsTable.defaultProps = {
  selectedGroups: [],
  setSelectedGroups: null,
  setGroupToDelete: null,
  shouldSelectRecipients: false
};

GroupsTable.propTypes = {
  onCopy: PropTypes.func.isRequired,
  setGroupToDelete: PropTypes.func,
  newGroupButton: PropTypes.node,
  selectedGroups: PropTypes.arrayOf(PropTypes.string),
  setSelectedGroups: PropTypes.func,
  shouldSelectRecipients: PropTypes.bool
};

export default GroupsTable;
