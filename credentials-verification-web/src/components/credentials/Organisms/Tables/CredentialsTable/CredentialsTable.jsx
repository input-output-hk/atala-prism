import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { Table } from 'antd';
import CellRenderer from '../../../../common/Atoms/CellRenderer/CellRenderer';
import { shortBackendDateFormatter } from '../../../../../helpers/formatters';
import { CREDENTIAL_PAGE_SIZE } from '../../../../../helpers/constants';
import RenderStudent from '../../../Molecules/RenderStudent/RenderStudent';
import holderDefaultAvatar from '../../../../../images/holder-default-avatar.svg';
import freeUniLogo from '../../../../../images/free-uni-logo.png';

import './_style.scss';
import CustomButton from '../../../../common/Atoms/CustomButton/CustomButton';

const getColumns = (viewText, sendCredentials, onView, issueCredential) => [
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
    key: 'studentname',
    render: ({ studentname }) => (
      <CellRenderer title="studentname" value={studentname} componentName="credentials" />
    )
  },
  {
    key: 'enrollmentdate',
    render: ({ enrollmentdate }) => (
      <CellRenderer
        title="admissionDate"
        value={shortBackendDateFormatter(enrollmentdate)}
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
    render: credential => {
      const { subject } = credential;
      return subject ? (
        <RenderStudent
          imageSrc={subject.avatar || holderDefaultAvatar}
          imageAlt={`${subject} avatar`}
          name={subject}
        />
      ) : (
        <CustomButton
          buttonProps={{
            theme: 'theme-outline',
            onClick: () => issueCredential(credential)
          }}
          buttonText={sendCredentials}
        />
      );
    }
  },
  {
    key: 'actions',
    render: credential => (
      <CustomButton
        buttonProps={{
          className: 'theme-link',
          onClick: () => onView(credential)
        }}
        buttonText={viewText}
      />
    )
  }
];

const CredentialsTable = ({
  issueCredential,
  credentials,
  credentialCount,
  offset,
  setOffset,
  onView
}) => {
  const { t } = useTranslation();

  return (
    <div className="CredentialsTable">
      <Table
        id="CredentialsTable"
        scroll={{ x: 1300, y: 600 }}
        columns={getColumns(
          t('actions.view'),
          t('credentials.sendCredentials'),
          onView,
          issueCredential
        )}
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
  setOffset: PropTypes.func.isRequired,
  onView: PropTypes.func.isRequired
};

export default CredentialsTable;
