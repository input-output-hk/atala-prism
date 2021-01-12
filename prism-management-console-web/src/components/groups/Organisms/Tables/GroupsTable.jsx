import React, { useState } from 'react';
import PropTypes from 'prop-types';
import InfiniteScrollTable from '../../../common/Organisms/Tables/InfiniteScrollTable';
import { getGroupColumns } from '../../../../helpers/tableDefinitions/groups';

import './_style.scss';

const GroupsTable = ({
  setGroupToDelete,
  groups,
  selectedGroups,
  setSelectedGroups,
  onPageChange
}) => {
  const [loading, setLoading] = useState(false);

  const getMoreData = () => {
    setLoading(true);
    return onPageChange().finally(() => setLoading(false));
  };

  const tableProps = {
    columns: getGroupColumns({ setGroupToDelete, setSelectedGroups }),
    data: groups,
    selectionType: !setSelectedGroups
      ? null
      : {
          selectedRowKeys: selectedGroups,
          type: 'checkbox',
          onChange: setSelectedGroups
        },
    loading,
    hasMore: false,
    getMoreData,
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
  setGroupToDelete: null
};

GroupsTable.propTypes = {
  setGroupToDelete: PropTypes.func,
  groups: PropTypes.arrayOf(PropTypes.object),
  onPageChange: PropTypes.func.isRequired,
  selectedGroups: PropTypes.arrayOf(PropTypes.string),
  setSelectedGroups: PropTypes.func,
  hasMore: PropTypes.bool.isRequired
};

export default GroupsTable;
