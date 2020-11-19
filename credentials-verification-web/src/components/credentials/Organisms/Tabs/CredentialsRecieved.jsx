import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { Row } from 'antd';
import { useTranslation } from 'react-i18next';
import CredentialsTable from '../Tables/CredentialsTable/CredentialsTable';
import EmptyComponent from '../../../common/Atoms/EmptyComponent/EmptyComponent';
import noCredentialsPicture from '../../../../images/noCredentials.svg';
import { CREDENTIALS_RECIEVED } from '../../../../helpers/constants';
import { credentialTabShape } from '../../../../helpers/propShapes';

const CredentialsRecieved = ({ showEmpty, tableProps, fetchCredentials, showCredentialData }) => {
  const { t } = useTranslation();
  const [loading, setLoading] = useState(false);

  const emptyProps = {
    photoSrc: noCredentialsPicture,
    model: t('credentials.title'),
    isFilter: showEmpty
  };

  const expandedTableProps = {
    ...tableProps,
    tab: CREDENTIALS_RECIEVED,
    onView: showCredentialData
  };

  const getMoreData = () => {
    setLoading(true);
    return fetchCredentials().finally(() => setLoading(false));
  };

  const renderEmptyComponent = !tableProps.credentials.length || showEmpty;

  return (
    <Row>
      {showEmpty || renderEmptyComponent ? (
        <EmptyComponent {...emptyProps} />
      ) : (
        <CredentialsTable getMoreData={getMoreData} loading={loading} {...expandedTableProps} />
      )}
    </Row>
  );
};

CredentialsRecieved.defaultProps = {
  showEmpty: false
};

CredentialsRecieved.propTypes = credentialTabShape;

export default CredentialsRecieved;
