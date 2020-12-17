import React from 'react';
import { Row } from 'antd';
import { useTranslation } from 'react-i18next';
import CredentialsTable from '../Tables/CredentialsTable/CredentialsTable';
import EmptyComponent from '../../../common/Atoms/EmptyComponent/EmptyComponent';
import noCredentialsPicture from '../../../../images/noCredentials.svg';
import { CREDENTIALS_RECEIVED } from '../../../../helpers/constants';
import { credentialTabShape } from '../../../../helpers/propShapes';
import SimpleLoading from '../../../common/Atoms/SimpleLoading/SimpleLoading';

const CredentialsReceived = ({ showEmpty, tableProps, showCredentialData, initialLoading }) => {
  const { t } = useTranslation();

  const emptyProps = {
    photoSrc: noCredentialsPicture,
    model: t('credentials.title'),
    isFilter: showEmpty
  };

  const expandedTableProps = {
    ...tableProps,
    tab: CREDENTIALS_RECEIVED,
    onView: showCredentialData
  };

  const renderEmptyComponent = !tableProps.credentials.length || showEmpty;

  const renderContent = () => {
    if (initialLoading) return <SimpleLoading size="md" />;
    if (renderEmptyComponent) return <EmptyComponent {...emptyProps} />;
    return <CredentialsTable {...expandedTableProps} />;
  };

  return <Row>{renderContent()}</Row>;
};

CredentialsReceived.defaultProps = {
  showEmpty: false,
  initialLoading: false
};

CredentialsReceived.propTypes = credentialTabShape;

export default CredentialsReceived;
