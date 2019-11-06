import React, { Fragment, useState } from 'react';
import { Icon } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import ConnectionsFilter from './Molecules/filter/ConnectionsFilter';
import ConnectionsTable from './Organisms/table/ConnectionsTable';
import CustomButton from '../common/Atoms/CustomButton/CustomButton';
import EmptyComponent from '../common/Atoms/EmptyComponent/EmptyComponent';
import noConnections from '../../images/noConnections.svg';
import QRModal from '../common/Organisms/Modals/QRModal/QRModal';
import Logger from '../../helpers/Logger';

import './_style.scss';

const ConnectionsButtons = () => {
  const { t } = useTranslation();

  return (
    <div className="ControlButtons">
      <CustomButton
        buttonProps={{
          className: 'theme-outline',
          onClick: () => Logger.info('placeholder function')
        }}
        buttonText={t('connections.buttons.bulk')}
        icon={<Icon type="plus" />}
      />
      <CustomButton
        buttonProps={{
          className: 'theme-secondary',
          onClick: () => Logger.info('placeholder function')
        }}
        buttonText={t('connections.buttons.manual')}
        icon={<Icon type="plus" />}
      />
    </div>
  );
};

const Connections = ({ tableProps, filterProps, inviteHolder }) => {
  const { t } = useTranslation();

  const [connectionToken, setConnectionToken] = useState('');
  const [QRModalIsOpen, showQRModal] = useState(false);

  const inviteHolderAndShowQR = () => {
    const cb = value => {
      setConnectionToken(value);
      showQRModal(true);
    };
    console.log('inviteHolderAndShowQR');
    inviteHolder(cb);
  };

  const emptyProps = {
    photoSrc: noConnections,
    photoAlt: t('connections.noConnections.photoAlt'),
    title: t('connections.noConnections.title'),
    subtitle: t('connections.noConnections.subtitle')
  };

  return (
    <Fragment>
      <div className="ContentHeader">
        <h1>{t('connections.title')}</h1>
        <ConnectionsButtons />
      </div>
      <ConnectionsFilter {...filterProps} />
      {tableProps.subjects.length ? (
        <ConnectionsTable inviteHolder={inviteHolderAndShowQR} {...tableProps} />
      ) : (
        <EmptyComponent {...emptyProps} />
      )}
      <QRModal
        visible={QRModalIsOpen}
        onCancel={() => showQRModal(false)}
        qrValue={connectionToken}
        tPrefix="recipients"
      />
    </Fragment>
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
