import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import CellRenderer from '../../../../common/Atoms/CellRenderer/CellRenderer';
import { backendDateFormatter } from '../../../../../helpers/formatters';
import RenderStudent from '../../../Molecules/RenderStudent/RenderStudent';
import InfiniteScrollTable from '../../../../common/Organisms/Tables/InfiniteScrollTable';
import holderDefaultAvatar from '../../../../../images/holder-default-avatar.svg';
import freeUniLogo from '../../../../../images/free-uni-logo.png';

import './_style.scss';
import CustomButton from '../../../../common/Atoms/CustomButton/CustomButton';
import { credentialShape } from '../../../../../helpers/propShapes';

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
        value={backendDateFormatter(enrollmentdate)}
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
  loading,
  getMoreData,
  hasMore,
  onView
}) => {
  const { t } = useTranslation();

  return (
    <div className="CredentialsTable">
      <InfiniteScrollTable
        columns={getColumns(
          t('actions.view'),
          t('credentials.sendCredentials'),
          onView,
          issueCredential
        )}
        data={credentials}
        loading={loading}
        getMoreData={getMoreData}
        hasMore={hasMore}
      />
    </div>
  );
};

CredentialsTable.defaultProps = {
  credentials: []
};

CredentialsTable.propTypes = {
  credentials: PropTypes.arrayOf(PropTypes.shape(credentialShape)),
  getMoreData: PropTypes.func.isRequired,
  loading: PropTypes.bool.isRequired,
  hasMore: PropTypes.bool.isRequired,
  onView: PropTypes.func.isRequired,
  issueCredential: PropTypes.func.isRequired
};

export default CredentialsTable;
