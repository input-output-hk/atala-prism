import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Row, Col, Input, Icon, Select } from 'antd';
import { PENDING_CONNECTION, CONNECTED, INDIVIDUAL_STATUSES } from '../../../../helpers/constants';

import './_style.scss';

const ConnectionsFilter = ({ fetchConnections }) => {
  const { t } = useTranslation();

  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [status, setStatus] = useState(t(''));

  useEffect(() => {
    fetchConnections(name, email, status);
  }, [name, email, status]);

  const statuses = [PENDING_CONNECTION, CONNECTED];
  const statusesValues = {
    PENDING_CONNECTION: INDIVIDUAL_STATUSES.created,
    CONNECTED: INDIVIDUAL_STATUSES.connected
  };

  return (
    <div className="FilterControls">
      <Row gutter={24}>
        <Col span={8}>
          <Input
            placeholder={t('contacts.filters.name')}
            prefix={<Icon type="search" />}
            onChange={({ target: { value } }) => setName(value)}
            allowClear
            value={name}
          />
        </Col>
        <Col span={8}>
          <Input
            placeholder={t('contacts.filters.email')}
            prefix={<Icon type="search" />}
            onChange={({ target: { value } }) => setEmail(value)}
            allowClear
            value={email}
          />
        </Col>
        <Col span={8}>
          <Select value={status} onChange={setStatus}>
            <Select.Option value="">{t('contacts.filters.status')}</Select.Option>
            {statuses.map(statusType => (
              <Select.Option key={statusType} value={statusesValues[statusType]}>
                {t(`holders.status.${statusType}`)}
              </Select.Option>
            ))}
          </Select>
        </Col>
      </Row>
    </div>
  );
};

ConnectionsFilter.propTypes = {
  fetchConnections: PropTypes.func.isRequired
};

export default ConnectionsFilter;
