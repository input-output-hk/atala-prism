import React from 'react';
import PropTypes from 'prop-types';
import { Table } from 'antd';
import CellRenderer from '../../../common/Atoms/CellRenderer/CellRenderer';
import StatusBadge from '../../Atoms/StatusBadge/StatusBadge';
import { shortBackendDateFormatter } from '../../../../helpers/formatters';
import {
  HOLDER_PAGE_SIZE,
  xScroll,
  CONNECTION_STATUSES,
  CONNECTION_STATUSES_TRANSLATOR
} from '../../../../helpers/constants';
import ActionButtons from '../../Atoms/ActionButtons/ActionButtons';
import holderDefaultAvatar from '../../../../images/holder-default-avatar.svg';

import './_style.scss';

const getColumns = ({ inviteHolder, isIssuer, setHolder }) => {
  const userColumn = [
    {
      key: 'avatar',
      render: ({ avatar }) => (
        <img
          style={{ width: '40px', height: '40px' }}
          src={avatar || holderDefaultAvatar}
          alt="Avatar"
        />
      )
    },
    { key: 'fullname', render: ({ fullname }) => fullname }
  ];

  const issuerInfo = [
    {
      key: 'identityNumber',
      render: ({ id }) => (
        <CellRenderer title="identityNumber" value={id} componentName="connections" />
      )
    },
    {
      key: 'admissionDate',
      render: ({ admissiondate }) => (
        <CellRenderer
          title="admissionDate"
          value={shortBackendDateFormatter(admissiondate)}
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
      key: 'connectionstatus',
      render: ({ connectionstatus }) => {
        const status = CONNECTION_STATUSES_TRANSLATOR[connectionstatus];
        return <StatusBadge status={status} />;
      }
    }
  ];

  const actionColumns = [
    {
      key: 'actions',
      render: holder => (
        <ActionButtons
          holder={holder}
          showQRButton={holder.connectionstatus === CONNECTION_STATUSES.invitationMissing}
          inviteHolder={inviteHolder}
          isIssuer={isIssuer}
          setHolder={setHolder}
        />
      )
    }
  ];

  const finalColumns = [];

  finalColumns.push(...userColumn);
  if (isIssuer()) finalColumns.push(...issuerInfo);
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
  isIssuer,
  setHolder
}) => (
  <div className="ConnectionsTable">
    <Table
      columns={getColumns({ inviteHolder, isIssuer, setHolder })}
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
  inviteHolder: PropTypes.func.isRequired,
  setHolder: PropTypes.func.isRequired,
  isIssuer: PropTypes.func.isRequired
};

export default ConnectionsTable;
