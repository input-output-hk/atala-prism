import React from 'react';
import { Table } from 'antd';
import PropTypes from 'prop-types';
import './_style.scss';
import { xScroll } from '../../../../helpers/constants';

const PaginatedTable = ({ columns, data, current, total, defaultPageSize, onChange }) => (
  <div className="PaginatedTable">
    <Table
      columns={columns}
      scroll={{ x: xScroll }}
      dataSource={data}
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
  total: 0
};

PaginatedTable.propTypes = {
  columns: PropTypes.arrayOf(PropTypes.object).isRequired,
  data: PropTypes.arrayOf(PropTypes.object),
  current: PropTypes.number,
  total: PropTypes.number,
  defaultPageSize: PropTypes.number.isRequired,
  onChange: PropTypes.func.isRequired
};

export default PaginatedTable;
