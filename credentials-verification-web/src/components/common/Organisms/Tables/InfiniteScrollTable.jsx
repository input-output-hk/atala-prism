import React, { useEffect } from 'react';
import { Table } from 'antd';
import PropTypes from 'prop-types';
import './_style.scss';
import { tableHeightKeys } from '../../../../helpers/propShapes';
import SimpleLoading from '../../Atoms/SimpleLoading/SimpleLoading';

const InfiniteScrollTable = ({
  selectionType,
  columns,
  data,
  handleSort,
  loading,
  searching,
  getMoreData,
  hasMore,
  rowKey,
  size
}) => {
  // This use effect handles the call for more data after
  // the scroll reaches the end
  useEffect(() => {
    const tableContent = document.querySelector('.InfiniteScrollTableContainer');

    const scrollListener = ({ target: { scrollHeight, clientHeight, scrollTop } }) => {
      const maxScroll = scrollHeight - clientHeight;
      if (hasMore && !loading && scrollTop === maxScroll) getMoreData();
    };

    tableContent.removeEventListener('scroll', scrollListener);
    tableContent.addEventListener('scroll', scrollListener);

    return () => tableContent.removeEventListener('scroll', scrollListener);
  }, [hasMore, loading]);

  return (
    <div
      className={`InfiniteScrollTableContainer ${handleSort ? '' : 'PaginatedTable'} table-${size}`}
    >
      <Table
        rowSelection={selectionType}
        columns={columns}
        dataSource={data}
        onChange={(_pagination, _filters, sorter) => {
          if (handleSort) return handleSort(sorter);
        }}
        pagination={false}
        rowKey={rowKey}
        footer={() => (searching || loading) && <SimpleLoading size="xs" />}
      />
    </div>
  );
};

InfiniteScrollTable.defaultProps = {
  data: [],
  selectionType: null,
  handleSort: null,
  searching: false,
  size: 'xl'
};

InfiniteScrollTable.propTypes = {
  selectionType: PropTypes.shape({ type: PropTypes.string }),
  columns: PropTypes.arrayOf(PropTypes.object).isRequired,
  data: PropTypes.arrayOf(PropTypes.object),
  handleSort: PropTypes.func,
  loading: PropTypes.bool.isRequired,
  searching: PropTypes.bool,
  getMoreData: PropTypes.func.isRequired,
  hasMore: PropTypes.bool.isRequired,
  rowKey: PropTypes.string.isRequired,
  size: PropTypes.oneOfType(tableHeightKeys)
};

export default InfiniteScrollTable;
