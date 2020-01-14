import React, { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes, { number } from 'prop-types';
import { Row, Col, Input, Icon, Select } from 'antd';
import { PENDING_CONNECTION, CONNECTED } from '../../../../helpers/constants';

import './_style.scss';

const ConnectionsFilter = ({ fetchConnections }) => {
  const { t } = useTranslation();

  const [identityNumber, setIdentityNumber] = useState('');
  const [name, setName] = useState('');
  const [status, setStatus] = useState(t(''));

  useEffect(() => {
    fetchConnections([], identityNumber, name, status);
  }, [identityNumber, name, status]);

  const statuses = [PENDING_CONNECTION, CONNECTED];

  return (
    <div className="FilterControls">
      <Row gutter={16}>
        <Col span={8}>
          <Input
            placeholder={t('connections.filters.identityNumber')}
            prefix={<Icon type="search" />}
            onChange={({ target: { value } }) => setIdentityNumber(value)}
            allowClear
            type={number}
            value={identityNumber}
          />
        </Col>
        <Col span={8}>
          <Input
            placeholder={t('connections.filters.name')}
            prefix={<Icon type="search" />}
            onChange={({ target: { value } }) => setName(value)}
            allowClear
            value={name}
          />
        </Col>
        <Col span={8}>
          <Select value={status} onChange={setStatus}>
            <Select.Option value="">{t('connections.filters.status')}</Select.Option>
            {statuses.map(statusType => (
              <Select.Option key={statusType} value={statusType}>
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
