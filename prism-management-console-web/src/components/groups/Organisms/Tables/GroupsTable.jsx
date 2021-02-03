import React from 'react';
import PropTypes from 'prop-types';
import InfiniteScrollTable from '../../../common/Organisms/Tables/InfiniteScrollTable';
import { getGroupColumns } from '../../../../helpers/tableDefinitions/groups';

import './_style.scss';

const GroupsTable = ({
  setGroupToDelete,
  groups,
  selectedGroups,
  setSelectedGroups,
  onCopy,
  shouldSelectRecipients
}) => {
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
    rowKey: 'name'
  };

  return (
    <div className="GroupTableContainer">
      <InfiniteScrollTable {...tableProps} />
    </div>
  );
};

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
  shouldSelectRecipients: PropTypes.bool
};

export default GroupsTable;
