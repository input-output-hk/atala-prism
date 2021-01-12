import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Row, Col, Input, Icon, Select } from 'antd';
import { PENDING_CONNECTION, CONNECTED } from '../../../../helpers/constants';

import './_style.scss';

const ConnectionsFilter = ({ searchText, setSearchText, status, setStatus, withStatus }) => {
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
        {withStatus && (
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
        )}
      </Row>
    </div>
  );
};

ConnectionsFilter.defaultProps = {
  searchText: undefined,
  status: undefined,
  setStatus: undefined,
  withStatus: true
};

ConnectionsFilter.propTypes = {
  setSearchText: PropTypes.func.isRequired,
  searchText: PropTypes.string,
  status: PropTypes.string,
  withStatus: PropTypes.bool,
  setStatus: PropTypes.func
};

export default ConnectionsFilter;
