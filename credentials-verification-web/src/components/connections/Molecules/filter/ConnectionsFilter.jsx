import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes, { number } from 'prop-types';
import { Row, Col, Input, Icon, Select } from 'antd';
import { PENDING_CONNECTION, CONNECTED } from '../../../../helpers/constants';

import './_style.scss';

const ConnectionsFilter = ({
  identityNumber,
  setIdentityNumber,
  name: userName,
  setName,
  status,
  setStatus
}) => {
  const { t } = useTranslation();

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
            value={userName}
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

ConnectionsFilter.defaultProps = {
  identityNumber: '',
  name: '',
  status: ''
};

ConnectionsFilter.propTypes = {
  identityNumber: PropTypes.string,
  setIdentityNumber: PropTypes.func.isRequired,
  name: PropTypes.string,
  setName: PropTypes.func.isRequired,
  status: PropTypes.string,
  setStatus: PropTypes.func.isRequired
};

export default ConnectionsFilter;
