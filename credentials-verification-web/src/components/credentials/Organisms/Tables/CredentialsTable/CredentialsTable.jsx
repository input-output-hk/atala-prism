import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { Table } from 'antd';
import { Link } from 'react-router-dom';
import CellRenderer from '../../../../common/Atoms/CellRenderer/CellRenderer';
import { shortDateFormatter } from '../../../../../helpers/formatters';
import { CREDENTIAL_PAGE_SIZE } from '../../../../../helpers/constants';
import RenderStudent from '../../../Molecules/RenderStudent/RenderStudent';
import holderDefaultAvatar from '../../../../../images/holder-default-avatar.svg';
import freeUniLogo from '../../../../../images/free-uni-logo.png';

import './_style.scss';
import CustomButton from '../../../../common/Atoms/CustomButton/CustomButton';

const getColumns = (viewText, sendCredentials) => [
  {
    key: 'icon',
    render: ({ icon, name }) => (
      <img
        style={{ width: '40px', height: '40px' }}
        src={icon || freeUniLogo}
        alt={`${name} icon`}
      />
    )
  },
  { key: 'title', render: ({ title }) => title },
  {
    key: 'id',
    render: ({ id }) => (
      <CellRenderer title="identityNumber" value={id} componentName="credentials" />
    )
  },
  {
    key: 'enrollmentdate',
    render: ({ enrollmentdate }) => (
      <CellRenderer
        title="admissionDate"
        value={shortDateFormatter(enrollmentdate)}
        componentName="credentials"
      />
    )
  },
  {
    key: 'groupname',
    render: ({ groupname }) => (
      <CellRenderer title="groupAssigned" value={groupname} componentName="credentials" />
    )
  },
  {
    key: 'subject',
    render: ({ subject }) =>
      subject ? (
        <RenderStudent
          imageSrc={subject.avatar || holderDefaultAvatar}
          imageAlt={`${subject} avatar`}
          name={subject}
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

const CredentialsTable = ({ credentials, credentialCount, offset, setOffset }) => {
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
  status: PropTypes.oneOf(['PENDING_CONNECTION', 'CONNECTED']),
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
  setOffset: PropTypes.func.isRequired
};

export default CredentialsTable;
