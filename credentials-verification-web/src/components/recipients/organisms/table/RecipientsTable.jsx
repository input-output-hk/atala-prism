import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { Table } from 'antd';
import { Link } from 'react-router-dom';
import CellRenderer from '../../../common/Atoms/CellRenderer/CellRenderer';
import StatusBadge from '../../../common/Atoms/StatusBadge/StatusBadge';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import { completeDateFormatter } from '../../../../helpers/formatters';
import { PENDING_INVITATION, HOLDER_PAGE_SIZE, xScroll } from '../../../../helpers/constants';

import './_style.scss';

const GetActionButtons = ({ id, showInviteButton, inviteHolder }) => {
  const { t } = useTranslation();

  return (
    <div className="ControlButtons">
      {showInviteButton && (
        <CustomButton
          onClick={() => inviteHolder(id)}
          buttonText={t('recipients.table.columns.invite')}
          theme="theme-link"
        />
      )}
      <CustomButton theme="theme-link" buttonText={t('recipients.table.columns.delete')} />
      <Link to={`/subject/${id}`}>{t('recipients.table.columns.view')}</Link>
    </div>
  );
};

GetActionButtons.propTypes = {
  id: PropTypes.string.isRequired,
  showInviteButton: PropTypes.bool.isRequired,
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
      <CellRenderer title="identityNumber" value={identityNumber} componentName="recipients" />
    )
  },
  {
    key: 'admissionDate',
    render: ({ admissionDate }) => (
      <CellRenderer
        title="admissionDate"
        value={completeDateFormatter(admissionDate)}
        componentName="recipients"
      />
    )
  },
  {
    key: 'email',
    render: ({ email }) => <CellRenderer title="email" value={email} componentName="recipients" />
  },
  {
    key: 'status',
    render: ({ status }) => <StatusBadge status={status} />
  },
  {
    key: 'actions',
    fixed: 'right',
    render: ({ id, status }) => (
      <GetActionButtons
        id={id}
        showInviteButton={status === PENDING_INVITATION}
        inviteHolder={inviteHolder}
      />
    )
  }
];

const RecipientsTable = ({ subjects, subjectCount, offset, setOffset, inviteHolder }) => (
  <div className="RecipientsTable">
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
  status: PropTypes.oneOf(['PENDING_CONNECTION', 'CONNECTED', 'PENDING_INVITATION']),
  id: PropTypes.string
};

RecipientsTable.defaultProps = {
  subjects: [],
  subjectCount: 0,
  offset: 0
};

RecipientsTable.propTypes = {
  subjects: PropTypes.arrayOf(PropTypes.shape(subjectShape)),
  subjectCount: PropTypes.number,
  offset: PropTypes.number,
  setOffset: PropTypes.func.isRequired,
  inviteHolder: PropTypes.func.isRequired
};

export default RecipientsTable;
