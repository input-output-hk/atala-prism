import React, { Fragment } from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { Card, Col, Collapse, Icon, Row } from 'antd';
import { connectionShape } from '../../../../helpers/propShapes';
import { shortDateFormatter } from '../../../../helpers/formatters';
import CellRenderer from '../../../common/Atoms/CellRenderer/CellRenderer';
import Connection from '../../Molecules/Connection/Connection';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';

import './_style.scss';

const ConnectionDetail = ({ user: { icon, name: userName, transactions }, date }) => {
  const { t } = useTranslation();

  const genExtra = () => <Icon type="caret-down" />;

  return (
    <div className="ConnectionDetail">
      <div className="ConnectionHeader">
        <img src={icon} alt={t('connections.detail.alt', userName)} />
        <div className="ConnectionUser">
          <h3>{userName}</h3>
          <p>{shortDateFormatter(date)}</p>
        </div>
      </div>
      <Collapse accordion className="TransactionsDetail">
        <Collapse.Panel
          header={t('connections.detail.transactions')}
          key="1"
          showArrow={false}
          extra={genExtra()}
        />
      </Collapse>
      {transactions.map(trans => (
        <div className="ConnectionLine">
          <p>Credential</p>
          <Connection {...trans} />
        </div>
      ))}
      <div className="ControlButtons">
        <CustomButton
          theme="theme-outline"
          buttonText={t('connections.detail.proofRequest')}
          onClick={() => console.log('placeholder function')}
          icon={<Icon type="plus" />}
        />
        <CustomButton
          theme="theme-secondary"
          buttonText={t('connections.detail.newCredential')}
          onClick={() => console.log('placeholder function')}
          icon={<Icon type="plus" />}
        />
      </div>
    </div>
  );
};

ConnectionDetail.propTypes = {
  user: PropTypes.shape(connectionShape.user).isRequired,
  date: connectionShape.date.isRequired
};

export default ConnectionDetail;
