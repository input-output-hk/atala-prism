import React from 'react';
import PropTypes from 'prop-types';
import i18n from 'i18next';
import { PAYMENT_PAGE_SIZE, AVATAR_WIDTH } from '../../../../helpers/constants';
import PaginatedTable from '../../../common/Organisms/Tables/PaginatedTable';
import { paymentShape } from '../../../../helpers/propShapes';
import CellRenderer from '../../../common/Atoms/CellRenderer/CellRenderer';
import { shortDateFormatter } from '../../../../helpers/formatters';

import './_style.scss';

const translationKeyPrefix = 'payment.table.columns';

const tp = chain => i18n.t(`${translationKeyPrefix}.${chain}`);

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
    render: ({ user: { name } }) => <CellRenderer title={tp('name')} value={name} />
  },
  {
    key: 'date',
    sorter: true,
    dataIndex: 'date',
    render: date => <CellRenderer title={tp('date')} value={shortDateFormatter(date)} />
  },
  {
    key: 'amount',
    sorter: true,
    render: ({ amount }) => <CellRenderer title={tp('amount')} value={amount} />
  },
  {
    key: 'currency',
    render: ({ currency }) => <CellRenderer title={tp('currency')} value={currency} />
  },
  {
    key: 'method',
    render: ({ method }) => <CellRenderer title={tp('method')} value={method} />
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
