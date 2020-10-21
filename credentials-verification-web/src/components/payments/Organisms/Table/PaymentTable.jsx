import React from 'react';
import PropTypes from 'prop-types';
import { PAYMENT_PAGE_SIZE, AVATAR_WIDTH } from '../../../../helpers/constants';
import PaginatedTable from '../../../common/Organisms/Tables/PaginatedTable';
import { paymentShape } from '../../../../helpers/propShapes';
import CellRenderer from '../../../common/Atoms/CellRenderer/CellRenderer';
import { shortDateFormatter } from '../../../../helpers/formatters';

import './_style.scss';

const getColumns = () => [
  {
    key: 'icon',
    width: AVATAR_WIDTH,
    dataIndex: 'user',
    render: ({ icon, name }) => (
      <img style={{ height: '40px', width: '40px' }} src={icon} alt={`${name} icon`} />
    )
  },
  {
    key: 'name',
    sorter: true,
    render: ({ user: { name } }) => (
      <CellRenderer title="name" componentName="payment" value={name} />
    )
  },
  {
    key: 'date',
    sorter: true,
    dataIndex: 'date',
    render: date => (
      <CellRenderer title="date" value={shortDateFormatter(date)} componentName="payment" />
    )
  },
  {
    key: 'amount',
    sorter: true,
    render: ({ amount }) => <CellRenderer title="amount" value={amount} componentName="payment" />
  },
  {
    key: 'currency',
    render: ({ currency }) => (
      <CellRenderer title="currency" value={currency} componentName="payment" />
    )
  },
  {
    key: 'method',
    render: ({ method }) => <CellRenderer title="method" value={method} componentName="payment" />
  }
];

const PaymentTable = ({ payments, paymentCount, offset, setOffset, changeSort }) => (
  <div className="PaymentTable">
    <PaginatedTable
      columns={getColumns()}
      data={payments}
      current={offset + 1}
      total={paymentCount}
      defaultPageSize={PAYMENT_PAGE_SIZE}
      onChange={pageToGoTo => setOffset(pageToGoTo - 1)}
      handleSort={changeSort}
    />
  </div>
);

PaymentTable.defaultProps = {
  payments: [],
  paymentCount: 0,
  offset: 0
};

PaymentTable.propTypes = {
  payments: PropTypes.arrayOf(paymentShape),
  paymentCount: PropTypes.number,
  offset: PropTypes.number,
  setOffset: PropTypes.func.isRequired,
  changeSort: PropTypes.func.isRequired
};

export default PaymentTable;
