import React, { useEffect } from 'react';
import { Table } from 'antd';
import PropTypes from 'prop-types';
import './_style.scss';
import { yScroll } from '../../../../helpers/constants';

const InfiniteScrollTable = ({
  selectionType,
  columns,
  data,
  handleSort,
  loading,
  getMoreData,
  hasMore,
  rowKey
}) => {
  // This use effect handles the call for more data after
  // the scroll reaches the end
  useEffect(() => {
    const tableContent = document.querySelector('.ant-table-body');

    const scrollListener = ({ target: { scrollHeight, clientHeight, scrollTop } }) => {
      const maxScroll = scrollHeight - clientHeight;
      if (hasMore && !loading && scrollTop === maxScroll) getMoreData();
    };

    tableContent.removeEventListener('scroll', scrollListener);
    tableContent.addEventListener('scroll', scrollListener);

    return () => tableContent.removeEventListener('scroll', scrollListener);
  }, [hasMore, loading]);

  return (
    <div className={handleSort ? '' : 'PaginatedTable'}>
      <Table
        rowSelection={selectionType}
        columns={columns}
        scroll={{ y: yScroll }}
        dataSource={data}
        onChange={(_pagination, _filters, sorter) => {
          if (handleSort) return handleSort(sorter);
        }}
        pagination={false}
        rowKey={rowKey}
      />
    </div>
  );
};

InfiniteScrollTable.defaultProps = {
  data: [],
  selectionType: null,
  handleSort: null
};

InfiniteScrollTable.propTypes = {
  selectionType: PropTypes.shape({ type: PropTypes.string }),
  columns: PropTypes.arrayOf(PropTypes.object).isRequired,
  data: PropTypes.arrayOf(PropTypes.object),
  handleSort: PropTypes.func,
  loading: PropTypes.bool.isRequired,
  getMoreData: PropTypes.func.isRequired,
  hasMore: PropTypes.bool.isRequired,
  rowKey: PropTypes.string.isRequired
};

export default InfiniteScrollTable;
