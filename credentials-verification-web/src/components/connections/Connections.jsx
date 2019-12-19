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
import Logger from '../../helpers/Logger';
import AddUserButtons from './Atoms/AddUsersButtons/AddUsersButtons';
import { drawerWidth } from '../../helpers/constants';
import CredentialListDetail from '../common/Organisms/Detail/CredentialListDetail';

import './_style.scss';

const Connections = ({ tableProps, filterProps, inviteHolder, isIssuer }) => {
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
    photoAlt: t('connections.noConnections.photoAlt'),
    title: t('connections.noConnections.title'),
    subtitle: t('connections.noConnections.subtitle')
  };

  const getStudentCredentials = connectionId => {
    const { getCredentials } = tableProps;
    return getCredentials(undefined, connectionId);
  };

  const viewConnection = connection => {
    const { createdat, fullname, connectionid } = connection;

    getStudentCredentials(connectionid)
      .then(transactions => {
        const formattedHolder = {
          user: { name: fullname, date: createdat },
          transactions
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
        {isIssuer() ? (
          <AddUserButtons />
        ) : (
          <CustomButton
            buttonProps={{
              className: 'theme-secondary',
              onClick: () => Logger.info('Tried to add a connection')
            }}
            buttonText={t('connections.buttons.newConnection')}
          />
        )}
      </div>
      <ConnectionsFilter {...filterProps} />
      {tableProps.subjects.length ? (
        <ConnectionsTable
          inviteHolder={inviteHolderAndShowQR}
          isIssuer={isIssuer}
          viewConnectionDetail={viewConnection}
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

const subjectShape = {
  avatar: PropTypes.string,
  name: PropTypes.string,
  identityNumber: PropTypes.number,
  admissionDate: PropTypes.number,
  email: PropTypes.string,
  status: PropTypes.oneOf(['PENDING_CONNECTION', 'CONNECTED']),
  id: PropTypes.string
};

Connections.propTypes = {
  tableProps: PropTypes.shape({
    subjects: PropTypes.arrayOf(PropTypes.shape(subjectShape)),
    subjectCount: PropTypes.number,
    offset: PropTypes.number,
    setOffset: PropTypes.func.isRequired,
    inviteHolder: PropTypes.func.isRequired,
    getCredentials: PropTypes.func.isRequired
  }).isRequired,
  filterProps: PropTypes.shape({
    userId: PropTypes.string,
    setUserId: PropTypes.func.isRequired,
    name: PropTypes.string,
    setName: PropTypes.func.isRequired,
    status: PropTypes.string,
    setStatus: PropTypes.func.isRequired
  }).isRequired,
  inviteHolder: PropTypes.func.isRequired,
  isIssuer: PropTypes.func.isRequired
};

export default Connections;
