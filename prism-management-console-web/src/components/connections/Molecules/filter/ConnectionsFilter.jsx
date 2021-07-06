import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { SearchOutlined } from '@ant-design/icons';
import { Input, Select } from 'antd';
import { PENDING_CONNECTION, CONNECTED } from '../../../../helpers/constants';

const ConnectionsFilter = ({ searchText, setSearchText, status, setStatus, withStatus }) => {
  const { t } = useTranslation();

  const statuses = [PENDING_CONNECTION, CONNECTED];

  return (
    <div className="FilterControls">
      <div className="ContactFilters">
        <Input
          placeholder={t('contacts.filters.search')}
          prefix={<SearchOutlined />}
          onChange={({ target: { value } }) => setSearchText(value)}
          allowClear
          value={searchText}
        />
        {withStatus && (
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
        )}
      </div>
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
