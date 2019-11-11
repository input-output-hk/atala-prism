import React from 'react';
import { Table } from 'antd';
import PropTypes from 'prop-types';
import './_style.scss';
import { xScroll } from '../../../../helpers/constants';

const PaginatedTable = ({
  selectionType,
  columns,
  data,
  current,
  total,
  defaultPageSize,
  onChange,
  handleSort
}) => (
  <div className={handleSort ? '' : 'PaginatedTable'}>
    <Table
      rowSelection={selectionType}
      columns={columns}
      scroll={{ x: xScroll }}
      dataSource={data}
      onChange={(_pagination, _filters, sorter) => {
        if (handleSort) return handleSort(sorter);
      }}
      pagination={{
        total,
        defaultCurrent: 1,
        current,
        defaultPageSize,
        onChange,
        hideOnSinglePage: true
      }}
    />
  </div>
);

PaginatedTable.defaultProps = {
  data: [],
  current: 0,
  total: 0,
  selectionType: null,
  handleSort: null
};

PaginatedTable.propTypes = {
  selectionType: PropTypes.shape({ type: PropTypes.string }),
  columns: PropTypes.arrayOf(PropTypes.object).isRequired,
  data: PropTypes.arrayOf(PropTypes.object),
  current: PropTypes.number,
  total: PropTypes.number,
  defaultPageSize: PropTypes.number.isRequired,
  onChange: PropTypes.func.isRequired,
  handleSort: PropTypes.func
};

export default PaginatedTable;
