import React, { useState } from 'react';
import PropTypes from 'prop-types';
import CellRenderer from '../../../common/Atoms/CellRenderer/CellRenderer';
import InfiniteScrollTable from '../../../common/Organisms/Tables/InfiniteScrollTable';
import ActionButtons from '../../Molecules/ActionButtons/ActionButtons';
import { ReactComponent as GroupIcon } from '../../../../images/icon-groups.svg';

import './_style.scss';

const getColumns = ({ setGroupToDelete, setGroup }) => {
  const componentName = 'groups';
  const fullInfo = !setGroup;

  const actionColumn = {
    key: 'actions',
    width: 300,
    render: ({ key, name }) => (
      <ActionButtons
        id={key}
        setGroupToDelete={() => setGroupToDelete({ id: key, name })}
        fullInfo={fullInfo}
      />
    )
  };

  const nameColumn = {
    key: 'groupName',
    width: 300,
    render: ({ name }) => (
      <CellRenderer title="groupName" componentName={componentName} value="" firstValue={name} />
    )
  };

  const iconColumn = {
    key: 'icon',
    width: 25,
    render: () => <GroupIcon />
  };

  return [iconColumn, nameColumn].concat(setGroupToDelete ? [actionColumn] : []);
};

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
    columns: getColumns({ setGroupToDelete, setSelectedGroups }),
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
