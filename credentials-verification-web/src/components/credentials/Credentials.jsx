import React, { useState } from 'react';
import { Row } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import CredentialsFilter from './Molecules/Filters/CredentialsFilter/CredentialsFilter';
import CredentialsTable from './Organisms/Tables/CredentialsTable/CredentialsTable';
import EmptyComponent from '../common/Atoms/EmptyComponent/EmptyComponent';
import CredentialSummaryDetail from '../common/Organisms/Detail/CredentialSummaryDetail';
import noCredentialsPicture from '../../images/noCredentials.svg';
import CredentialButtons from './Atoms/Buttons/CredentialButtons';
import { subjectShape } from '../../helpers/propShapes';
import { shortBackendDateFormatter } from '../../helpers/formatters';

import './_style.scss';

const Credentials = ({ showEmpty, tableProps, filterProps, noCredentials, fetchCredentials }) => {
  const { t } = useTranslation();
  const [currentCredential, setCurrentCredential] = useState({});
  const [showDrawer, setShowDrawer] = useState(false);
  const [loading, setLoading] = useState(false);

  const getMoreData = () => {
    setLoading(true);
    return fetchCredentials().finally(() => setLoading(false));
  };

  const emptyProps = {
    photoSrc: noCredentialsPicture,
    model: t('credentials.title'),
    isFilter: noCredentials,
    button: <CredentialButtons />
  };

  const showCredentialData = ({ title, graduationdate, enrollmentdate, studentname }) => {
    const credentialToShow = {
      graduationDate: shortBackendDateFormatter(graduationdate),
      startDate: shortBackendDateFormatter(enrollmentdate),
      student: {
        fullname: studentname
      },
      lg: 24,
      title
    };

    setCurrentCredential(credentialToShow);
    setShowDrawer(true);
  };

  const expandedTableProps = Object.assign({}, tableProps, { onView: showCredentialData });
  const renderEmptyComponent = !tableProps.credentials.length || noCredentials;

  return (
    <div className="Wrapper PageContainer">
      <CredentialSummaryDetail
        drawerInfo={{
          title: t('credentials.detail.title'),
          onClose: () => setShowDrawer(false),
          visible: showDrawer
        }}
        credentialData={currentCredential}
      />
      <div className="ContentHeader">
        <h1>{t('credentials.title')}</h1>
        <CredentialButtons />
      </div>
      <CredentialsFilter {...filterProps} fetchCredentials={fetchCredentials} />
      <Row>
        {showEmpty || renderEmptyComponent ? (
          <EmptyComponent {...emptyProps} />
        ) : (
          <CredentialsTable getMoreData={getMoreData} loading={loading} {...expandedTableProps} />
        )}
      </Row>
    </div>
  );
};

Credentials.defaultProps = {
  showEmpty: false
};

Credentials.propTypes = {
  noCredentials: PropTypes.bool.isRequired,
  fetchCredentials: PropTypes.func.isRequired,
  tableProps: PropTypes.shape({
    subjects: PropTypes.arrayOf(PropTypes.shape(subjectShape)),
    credentials: PropTypes.arrayOf(PropTypes.shape())
  }).isRequired,
  filterProps: PropTypes.shape({
    credentialTypes: PropTypes.arrayOf(PropTypes.string),
    categories: PropTypes.arrayOf(PropTypes.string),
    groups: PropTypes.arrayOf(PropTypes.string)
  }).isRequired,
  showEmpty: PropTypes.bool,
  redirector: PropTypes.shape({ redirectToNewCredential: PropTypes.func }).isRequired
};

export default Credentials;
