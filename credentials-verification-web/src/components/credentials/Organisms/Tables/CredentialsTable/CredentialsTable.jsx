import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { Table } from 'antd';
import { Link } from 'react-router-dom';
import CellRenderer from '../../../../common/Atoms/CellRenderer/CellRenderer';
import { shortDateFormatter } from '../../../../../helpers/formatters';
import { CREDENTIAL_PAGE_SIZE } from '../../../../../helpers/constants';
import RenderStudent from '../../../Molecules/RenderStudent/RenderStudent';

import './_style.scss';
import CustomButton from '../../../../common/Atoms/CustomButton/CustomButton';

const getColumns = (viewText, sendCredentials) => [
  {
    key: 'icon',
    render: ({ icon, name }) => (
      <img style={{ width: '40px', height: '40px' }} src={icon} alt={`${name} icon`} />
    )
  },
  { key: 'name', render: ({ name }) => name },
  {
    key: 'identityNumber',
    render: ({ identityNumber }) => (
      <CellRenderer title="identityNumber" value={identityNumber} componentName="credentials" />
    )
  },
  {
    key: 'admissionDate',
    render: ({ admissionDate }) => (
      <CellRenderer
        title="admissionDate"
        value={shortDateFormatter(admissionDate)}
        componentName="credentials"
      />
    )
  },
  {
    key: 'groupId',
    render: ({ groupId }) => (
      <CellRenderer title="groupAssigned" value={groupId} componentName="credentials" />
    )
  },
  {
    key: 'student',
    render: ({ student }) =>
      student ? (
        <RenderStudent
          imageSrc={student.avatar}
          imageAlt={`${student.name} avatar`}
          name={student.name}
        />
      ) : (
        <CustomButton
          buttonText={sendCredentials}
          theme="theme-outline"
          onClick={() => console.log('memes')}
        />
      )
  },
  {
    key: 'actions',
    fixed: 'right',
    render: ({ id }) => (
      <div className="ControlButtons">
        <Link to={`credential/${id}`}>{viewText}</Link>
      </div>
    )
  }
];

const CredentialsTable = ({ credentials, credentialCount, offset, setOffset, inviteHolder }) => {
  const { t } = useTranslation();

  return (
    <div className="CredentialsTable">
      <Table
        id="CredentialsTable"
        scroll={{ x: 1300 }}
        columns={getColumns(t('actions.view'), t('credentials.sendCredentials'))}
        dataSource={credentials}
        pagination={{
          total: credentialCount,
          defaultCurrent: 1,
          current: offset + 1,
          defaultPageSize: CREDENTIAL_PAGE_SIZE,
          onChange: pageToGoTo => setOffset(pageToGoTo - 1)
        }}
      />
    </div>
  );
};

const credentialshape = {
  icon: PropTypes.string,
  name: PropTypes.string,
  identityNumber: PropTypes.number,
  admissionDate: PropTypes.number,
  email: PropTypes.string,
  status: PropTypes.oneOf(['PENDING_CONNECTION', 'CONNECTED', 'PENDING_INVITATION']),
  id: PropTypes.string
};

CredentialsTable.defaultProps = {
  credentials: [],
  credentialCount: 0,
  offset: 0
};

CredentialsTable.propTypes = {
  credentials: PropTypes.arrayOf(PropTypes.shape(credentialshape)),
  credentialCount: PropTypes.number,
  offset: PropTypes.number,
  setOffset: PropTypes.func.isRequired,
  inviteHolder: PropTypes.func.isRequired
};

export default CredentialsTable;
