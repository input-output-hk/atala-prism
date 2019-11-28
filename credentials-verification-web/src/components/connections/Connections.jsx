import React, { useState } from 'react';
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
import { ISSUER } from '../../helpers/constants';

import './_style.scss';

const Connections = ({ tableProps, filterProps, inviteHolder }) => {
  const { t } = useTranslation();

  const [connectionToken, setConnectionToken] = useState('');
  const [QRModalIsOpen, showQRModal] = useState(false);

  const isIssuer = localStorage.getItem('userRole') === ISSUER;

  const inviteHolderAndShowQR = () => {
    const cb = value => {
      setConnectionToken(value);
      showQRModal(true);
    };
    inviteHolder(cb);
  };

  const emptyProps = {
    photoSrc: noConnections,
    photoAlt: t('connections.noConnections.photoAlt'),
    title: t('connections.noConnections.title'),
    subtitle: t('connections.noConnections.subtitle')
  };

  return (
    <div className="Wrapper">
      <div className="ContentHeader">
        <h1>{t('connections.title')}</h1>
        {isIssuer ? (
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
          {...tableProps}
          isIssuer={isIssuer}
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
    inviteHolder: PropTypes.func.isRequired
  }).isRequired,
  filterProps: PropTypes.shape({
    userId: PropTypes.string,
    setUserId: PropTypes.func.isRequired,
    name: PropTypes.string,
    setName: PropTypes.func.isRequired,
    status: PropTypes.string,
    setStatus: PropTypes.func.isRequired
  }).isRequired,
  inviteHolder: PropTypes.func.isRequired
};

export default Connections;
