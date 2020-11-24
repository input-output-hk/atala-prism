import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Row, Col, Input, Icon, Select } from 'antd';
import { PENDING_CONNECTION, CONNECTED } from '../../../../helpers/constants';

import './_style.scss';

const ConnectionsFilter = ({ searchText, setSearchText, status, setStatus }) => {
  const { t } = useTranslation();

  const statuses = [PENDING_CONNECTION, CONNECTED];

  return (
    <div className="FilterControls">
      <Row className="ContactFilters" gutter={20}>
        <Col span={12}>
          <Input
            placeholder={t('contacts.filters.search')}
            prefix={<Icon type="search" />}
            onChange={({ target: { value } }) => setSearchText(value)}
            allowClear
            value={searchText}
          />
        </Col>
        <Col span={12}>
          <Select
            value={status}
            onChange={setStatus}
            allowClear
            placeholder={t('contacts.filters.status')}
          >
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
  searchText: undefined,
  status: undefined
};

ConnectionsFilter.propTypes = {
  searchText: PropTypes.string,
  setSearchText: PropTypes.func.isRequired,
  status: PropTypes.string,
  setStatus: PropTypes.func.isRequired
};

export default ConnectionsFilter;
