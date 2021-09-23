import React, { useEffect } from 'react';
import { ConfigProvider, Empty, Table } from 'antd';
import PropTypes from 'prop-types';
import SimpleLoading from '../../Atoms/SimpleLoading/SimpleLoading';
import './_style.scss';

const InfiniteScrollTable = ({
  selectionType,
  columns,
  data,
  handleSort,
  loading,
  searching,
  getMoreData,
  hasMore,
  rowKey
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
  }, [hasMore, loading, getMoreData]);

  const showFooter = Boolean(data.length && (searching || loading));

  const renderLoadingRow = () => <SimpleLoading size="xs" />;
  // TODO: add i18n
  const renderEmpty = () => <Empty description="No results" />;

  return (
    <div className={`InfiniteScrollTable ${handleSort ? '' : 'PaginatedTable'}`}>
      <ConfigProvider renderEmpty={searching ? renderLoadingRow : renderEmpty}>
        <Table
          rowSelection={selectionType}
          columns={columns}
          dataSource={data}
          onChange={(_pagination, _filters, sorter) => {
            if (handleSort) return handleSort(sorter);
          }}
          pagination={false}
          rowKey={rowKey}
          footer={showFooter && renderLoadingRow}
        />
      </ConfigProvider>
    </div>
  );
};

InfiniteScrollTable.defaultProps = {
  data: [],
  selectionType: null,
  handleSort: null,
  searching: false
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
  rowKey: PropTypes.string.isRequired
};

export default InfiniteScrollTable;
