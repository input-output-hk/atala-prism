import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { Table } from 'antd';
import CellRenderer from '../../../common/Atoms/CellRenderer/CellRenderer';
import StatusBadge from '../../Atoms/StatusBadge/StatusBadge';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import { longDateFormatter } from '../../../../helpers/formatters';
import { HOLDER_PAGE_SIZE, xScroll, PENDING_CONNECTION } from '../../../../helpers/constants';

import './_style.scss';

const GetActionButtons = ({ id, showQRButton, inviteHolder }) => {
  const { t } = useTranslation();

  return (
    <div className="ControlButtons">
      {showQRButton && (
        <CustomButton
          buttonProps={{
            onClick: () => inviteHolder(id),
            className: 'theme-link'
          }}
          buttonText={t('connections.table.columns.invite')}
        />
      )}
      <CustomButton
        buttonProps={{ className: 'theme-link' }}
        buttonText={t('connections.table.columns.delete')}
      />
      <CustomButton
        buttonProps={{
          className: 'theme-link'
        }}
        buttonText={t('connections.table.columns.view')}
      />
    </div>
  );
};

GetActionButtons.propTypes = {
  id: PropTypes.string.isRequired,
  showQRButton: PropTypes.bool.isRequired,
  inviteHolder: PropTypes.func.isRequired
};

const getColumns = inviteHolder => [
  {
    key: 'avatar',
    render: ({ avatar }) => (
      <img style={{ width: '40px', height: '40px' }} src={avatar} alt="imagecita" />
    )
  },
  { key: 'name', render: ({ name }) => name },
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
        value={longDateFormatter(admissionDate)}
        componentName="connections"
      />
    )
  },
  {
    key: 'email',
    render: ({ email }) => <CellRenderer title="email" value={email} componentName="connections" />
  },
  {
    key: 'status',
    render: ({ status }) => <StatusBadge status={status} />
  },
  {
    key: 'actions',
    render: ({ id, status }) => (
      <GetActionButtons
        id={id}
        showQRButton={status === PENDING_CONNECTION}
        inviteHolder={inviteHolder}
      />
    )
  }
];

const ConnectionsTable = ({ subjects, subjectCount, offset, setOffset, inviteHolder }) => (
  <div className="ConnectionsTable">
    <Table
      columns={getColumns(inviteHolder)}
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
