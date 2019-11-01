import React, { Fragment } from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { Card, Col, Collapse, Icon, Row } from 'antd';
import { connectionShape } from '../../../../helpers/propShapes';
import { shortDateFormatter } from '../../../../helpers/formatters';
import CellRenderer from '../../../common/Atoms/CellRenderer/CellRenderer';
import Connection from '../../Molecules/Connection/Connection';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';

const ConnectionDetail = ({ user: { icon, name: userName, transactions }, date }) => {
  const { t } = useTranslation();

  return (
    <Card>
      <Fragment>
        <Col>
          <img src={icon} alt={t('connections.detail.alt', userName)} />
        </Col>
        <Col>
          <Row>{userName}</Row>
          <Row>{shortDateFormatter(date)}</Row>
        </Col>
        <Collapse accordion>
          <Collapse.Panel header={t('connections.detail.transactions')} key="1">
            {transactions.map(trans => (
              <CellRenderer
                componentName="connections"
                value={<Connection {...trans} />}
                title="type"
              />
            ))}
          </Collapse.Panel>
        </Collapse>
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
      </Fragment>
    </Card>
  );
};

ConnectionDetail.propTypes = {
  user: PropTypes.shape(connectionShape.user).isRequired,
  date: connectionShape.date.isRequired
};

export default ConnectionDetail;
