import React, { useState } from 'react';
import { Checkbox, Row } from 'antd';
import { useTranslation } from 'react-i18next';
import { PulseLoader } from 'react-spinners';
import PropTypes from 'prop-types';
import CredentialsFilter from './Molecules/Filters/CredentialsFilter/CredentialsFilter';
import CredentialsTable from './Organisms/Tables/CredentialsTable/CredentialsTable';
import EmptyComponent from '../common/Atoms/EmptyComponent/EmptyComponent';
import CredentialSummaryDetail from '../common/Organisms/Detail/CredentialSummaryDetail';
import noCredentialsPicture from '../../images/noCredentials.svg';
import CredentialButtons from './Atoms/Buttons/CredentialButtons';
import { contactShape } from '../../helpers/propShapes';
import { shortBackendDateFormatter } from '../../helpers/formatters';
import CreateCredentialsButton from './Atoms/Buttons/CreateCredentialsButton';

import './_style.scss';

const Credentials = ({
  showEmpty,
  tableProps,
  filterProps,
  fetchCredentials,
  bulkActionsProps,
  loadingSelection
}) => {
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
    isFilter: showEmpty,
    button: <CreateCredentialsButton />
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

  const { selectedCredentials } = tableProps;
  const { selectAll, indeterminateSelectAll, toggleSelectAll } = bulkActionsProps;
  const expandedTableProps = {
    ...tableProps,
    onView: showCredentialData
  };

  const renderEmptyComponent = !tableProps.credentials.length || showEmpty;

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
        <div>
          <h1>{t('credentials.title')}</h1>
          <h3>{t('credentials.info')}</h3>
        </div>
        <CredentialButtons
          {...bulkActionsProps}
          disableSign={!selectedCredentials?.length || loadingSelection}
          disableSend={!selectedCredentials?.length || loadingSelection}
        />
      </div>
      <CredentialsFilter {...filterProps} fetchCredentials={fetchCredentials} />
      <Checkbox
        indeterminate={indeterminateSelectAll}
        className="checkboxCredential"
        onChange={toggleSelectAll}
        checked={selectAll}
        disabled={loadingSelection}
      >
        {loadingSelection ? (
          <PulseLoader size={3} color="#FFAEB3" />
        ) : (
          t('credentials.actions.selectAll')
        )}
      </Checkbox>
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
  fetchCredentials: PropTypes.func.isRequired,
  tableProps: PropTypes.shape({
    subjects: PropTypes.arrayOf(PropTypes.shape(contactShape)),
    credentials: PropTypes.arrayOf(PropTypes.shape()),
    selectedCredentials: PropTypes.arrayOf(PropTypes.string)
  }).isRequired,
  filterProps: PropTypes.shape({
    credentialTypes: PropTypes.arrayOf(PropTypes.string),
    categories: PropTypes.arrayOf(PropTypes.string),
    groups: PropTypes.arrayOf(PropTypes.string)
  }).isRequired,
  showEmpty: PropTypes.bool,
  bulkActionsProps: PropTypes.shape({
    signSelectedCredentials: PropTypes.func.isRequired,
    sendSelectedCredentials: PropTypes.func.isRequired,
    toggleSelectAll: PropTypes.func.isRequired,
    selectAll: PropTypes.bool.isRequired,
    indeterminateSelectAll: PropTypes.bool.isRequired
  }).isRequired,
  loadingSelection: PropTypes.bool.isRequired
};

export default Credentials;
