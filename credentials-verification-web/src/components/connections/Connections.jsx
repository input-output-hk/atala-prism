import React, { useState, useEffect } from 'react';
import { Drawer, message } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import ConnectionsFilter from './Molecules/filter/ConnectionsFilter';
import ConnectionsTable from './Organisms/table/ConnectionsTable';
import CustomButton from '../common/Atoms/CustomButton/CustomButton';
import EmptyComponent from '../common/Atoms/EmptyComponent/EmptyComponent';
import noConnections from '../../images/noConnections.svg';
import QRModal from '../common/Organisms/Modals/QRModal/QRModal';
import AddUserButtons from './Atoms/AddUsersButtons/AddUsersButtons';
import { drawerWidth } from '../../helpers/constants';
import CredentialListDetail from '../common/Organisms/Detail/CredentialListDetail';
import { subjectShape } from '../../helpers/propShapes';

import './_style.scss';
import { withRedirector } from '../providers/withRedirector';

const Connections = ({
  redirector: { redirectToBulkImport },
  tableProps,
  inviteHolder,
  isIssuer,
  handleHoldersRequest
}) => {
  const { t } = useTranslation();

  const [connectionToken, setConnectionToken] = useState('');
  const [QRModalIsOpen, showQRModal] = useState(false);
  const [currentConnection, setCurrentConnection] = useState({});
  const [showDrawer, setShowDrawer] = useState();

  useEffect(() => {
    if (!showDrawer) setCurrentConnection({});
  }, [showDrawer]);

  const inviteHolderAndShowQR = async holderId => {
    const token = await inviteHolder(holderId);
    setConnectionToken(token);
    showQRModal(true);
  };

  const emptyProps = {
    photoSrc: noConnections,
    model: t('connections.title')
  };

  const getStudentCredentials = connectionId => {
    const { getCredentials } = tableProps;
    return getCredentials(undefined, connectionId);
  };

  const viewConnection = connection => {
    const { admissiondate, avatar, createdat, fullname, connectionid } = connection;

    getStudentCredentials(connectionid)
      .then(transactions => {
        const formattedHolder = {
          user: { icon: avatar, name: fullname, date: createdat },
          transactions,
          date: admissiondate
        };

        setCurrentConnection(formattedHolder);
        setShowDrawer(true);
      })
      .catch(() => message.error(t('errors.errorGetting', { model: 'Credentials' })));
  };

  return (
    <div className="Wrapper">
      <Drawer
        title={t('connections.detail.title')}
        placement="right"
        onClose={() => setShowDrawer(false)}
        visible={showDrawer}
        width={drawerWidth}
        destroyOnClose
      >
        {showDrawer && <CredentialListDetail {...currentConnection} />}
      </Drawer>
      <div className="ContentHeader">
        <h1>{t('connections.title')}</h1>
        <AddUserButtons isIssuer={isIssuer} />
      </div>
      <ConnectionsFilter fetchConnections={handleHoldersRequest} />
      {tableProps.subjects.length ? (
        <ConnectionsTable
          inviteHolder={inviteHolderAndShowQR}
          isIssuer={isIssuer}
          viewConnectionDetail={viewConnection}
          handleHoldersRequest={handleHoldersRequest}
          {...tableProps}
        />
      ) : (
        <EmptyComponent {...emptyProps} />
      )}
      <QRModal
        visible={QRModalIsOpen}
        onCancel={() => showQRModal(false)}
        qrValue={connectionToken}
        tPrefix="connections"
      />
    </div>
  );
};

Connections.propTypes = {
  redirector: PropTypes.shape({ redirectToBulkImport: PropTypes.func }).isRequired,
  handleHoldersRequest: PropTypes.func.isRequired,
  tableProps: PropTypes.shape({
    subjects: PropTypes.arrayOf(PropTypes.shape(subjectShape)),
    getCredentials: PropTypes.func.isRequired,
    hasMore: PropTypes.bool.isRequired
  }).isRequired,
  inviteHolder: PropTypes.func.isRequired,
  isIssuer: PropTypes.func.isRequired,
  redirectToBulkImport: PropTypes.func.isRequired
};

export default withRedirector(Connections);
