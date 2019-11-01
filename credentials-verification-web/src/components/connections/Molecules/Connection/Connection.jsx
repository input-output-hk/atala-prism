import React from 'react';
import { Col, Row } from 'antd';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { shortDateFormatter } from '../../../../helpers/formatters';

const Connection = ({ icon, type, date, id }) => {
  const { t } = useTranslation();
  return (
    <Row>
      <Col>{<img src={icon} alt={t('connections.detail.iconAlt')} />}</Col>
      <Col>
        <Row>{type}</Row>
        <Row>{shortDateFormatter(date)}</Row>
      </Col>
      <Link to={`/credential/${id}`}>{t('actions.view')}</Link>
    </Row>
  );
};

export default Connection;
