import React from 'react';
import PropTypes from 'prop-types';
import { Table } from 'antd';
import CellRenderer from '../../../common/Atoms/CellRenderer/CellRenderer';
import StatusBadge from '../../Atoms/StatusBadge/StatusBadge';
import { longDateFormatter, shortBackendDateFormatter } from '../../../../helpers/formatters';
import { HOLDER_PAGE_SIZE, xScroll, PENDING_CONNECTION } from '../../../../helpers/constants';
import ActionButtons from '../../Atoms/ActionButtons/ActionButtons';

import './_style.scss';

const getColumns = (inviteHolder, isIssuer) => {
  const userColumn = [
    {
      key: 'avatar',
      render: ({ avatar }) => (
        <img style={{ width: '40px', height: '40px' }} src={avatar} alt="imagecita" />
      )
    },
    { key: 'name', render: ({ name }) => name }
  ];

  const issuerInfo = [
    {
      key: 'identityNumber',
      render: ({ identityNumber }) => (
        <CellRenderer title="identityNumber" value={identityNumber} componentName="connections" />
      )
    },
    {
      key: 'admissionDate',
      render: ({ admissionDate }) => (
        <CellRenderer
          title="admissionDate"
          value={shortBackendDateFormatter(admissionDate)}
          componentName="connections"
        />
      )
    }
  ];

  const genericColumns = [
    {
      key: 'email',
      render: ({ email }) => (
        <CellRenderer title="email" value={email} componentName="connections" />
      )
    },
    {
      key: 'status',
      render: ({ status }) => <StatusBadge status={status} />
    }
  ];

  const actionColumns = [
    {
      key: 'actions',
      render: ({ id, status }) => (
        <ActionButtons
          id={id}
          showQRButton={status === PENDING_CONNECTION}
          inviteHolder={inviteHolder}
          isIssuer={isIssuer}
        />
      )
    }
  ];

  const finalColumns = [];

  finalColumns.push(...userColumn);
  if (isIssuer) finalColumns.push(...issuerInfo);
  finalColumns.push(...genericColumns);
  finalColumns.push(...actionColumns);

  return finalColumns;
};

const ConnectionsTable = ({
  subjects,
  subjectCount,
  offset,
  setOffset,
  inviteHolder,
  isIssuer
}) => (
  <div className="ConnectionsTable">
    <Table
      columns={getColumns(inviteHolder, isIssuer)}
      dataSource={subjects}
      scroll={{ x: xScroll }}
      pagination={{
        total: subjectCount,
        defaultCurrent: 1,
        current: offset + 1,
        defaultPageSize: HOLDER_PAGE_SIZE,
        onChange: pageToGoTo => setOffset(pageToGoTo - 1)
      }}
    />
  </div>
);

const subjectShape = {
  avatar: PropTypes.string,
  name: PropTypes.string,
  identityNumber: PropTypes.number,
  admissionDate: PropTypes.number,
  email: PropTypes.string,
  status: PropTypes.oneOf(['PENDING_CONNECTION', 'CONNECTED']),
  id: PropTypes.string
};

ConnectionsTable.defaultProps = {
  subjects: [],
  subjectCount: 0,
  offset: 0
};

ConnectionsTable.propTypes = {
  subjects: PropTypes.arrayOf(PropTypes.shape(subjectShape)),
  subjectCount: PropTypes.number,
  offset: PropTypes.number,
  setOffset: PropTypes.func.isRequired,
  inviteHolder: PropTypes.func.isRequired
};

export default ConnectionsTable;
