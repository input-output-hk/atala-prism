import React, { useEffect } from 'react';
import { ConfigProvider, Table } from 'antd';
import PropTypes from 'prop-types';
import SimpleLoading from '../../Atoms/SimpleLoading/SimpleLoading';
import './_style.scss';

const InfiniteScrollTable = ({
  selectionType,
  columns,
  data,
  handleSort,
  loading,
  fetchingMore,
  getMoreData,
  hasMore,
  rowKey,
  renderEmpty
}) => {
  // This use effect handles the call for more data after
  // the scroll reaches the end
  useEffect(() => {
    const tableContent = document.querySelector('.InfiniteScrollTableContainer');
    const scrollListener = ({ target: { scrollHeight, clientHeight, scrollTop } }) => {
      const maxScroll = scrollHeight - clientHeight;
      // TODO: there must be a better way to do this. Also, check if Antd has some virtualized table
      //  or list component, standard table wont work in this setup as infinite list when there is a
      //  lot of rows
      // scrollTop was half pixel more than maxScroll?
      if (hasMore && !loading && scrollTop >= maxScroll) getMoreData();
    };

    tableContent.removeEventListener('scroll', scrollListener);
    tableContent.addEventListener('scroll', scrollListener);

    return () => tableContent.removeEventListener('scroll', scrollListener);
  }, [hasMore, loading, getMoreData]);

  const renderLoadingRow = () => <SimpleLoading />;

  return (
    <div className={`InfiniteScrollTable ${handleSort ? '' : 'PaginatedTable'}`}>
      <ConfigProvider renderEmpty={() => (fetchingMore ? '' : renderEmpty)}>
        <Table
          rowSelection={selectionType}
          columns={columns}
          dataSource={data}
          onChange={(_pagination, _filters, sorter) => {
            if (handleSort) return handleSort(sorter);
          }}
          pagination={false}
          rowKey={rowKey}
          footer={fetchingMore && !loading && renderLoadingRow}
          loading={loading && { indicator: <SimpleLoading /> }}
        />
      </ConfigProvider>
    </div>
  );
};

InfiniteScrollTable.defaultProps = {
  data: [],
  selectionType: null,
  handleSort: null,
  fetchingMore: false
};

InfiniteScrollTable.propTypes = {
  selectionType: PropTypes.shape({ type: PropTypes.string }),
  columns: PropTypes.arrayOf(PropTypes.object).isRequired,
  data: PropTypes.arrayOf(PropTypes.object),
  handleSort: PropTypes.func,
  loading: PropTypes.bool.isRequired,
  fetchingMore: PropTypes.bool,
  getMoreData: PropTypes.func.isRequired,
  hasMore: PropTypes.bool.isRequired,
  rowKey: PropTypes.string.isRequired,
  renderEmpty: PropTypes.oneOfType([PropTypes.func, PropTypes.node]).isRequired
};

export default InfiniteScrollTable;
