import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { DownOutlined, SearchOutlined } from '@ant-design/icons';
import { Input, Select } from 'antd';
import {
  PENDING_CONNECTION,
  CONNECTED,
  SORTING_DIRECTIONS,
  CONTACT_SORTING_KEYS
} from '../../../../helpers/constants';
import CustomInputGroup from '../../../common/Atoms/CustomInputGroup/CustomInputGroup';
import CustomDatePicker from '../../../common/Atoms/CustomDatePicker/CustomDatePicker';
import { useContactUiState } from '../../../../hooks/useContactStore';

const ConnectionsFilter = ({ fullFilters }) => {
  const { t } = useTranslation();
  const {
    sortingDirection,
    setSortingBy,
    setFilterValue,
    toggleSortDirection
  } = useContactUiState();

  const statuses = [PENDING_CONNECTION, CONNECTED];
  const isAscending = sortingDirection === SORTING_DIRECTIONS.ascending;

  const datePickerProps = {
    placeholder: t('contacts.filters.createdAt'),
    suffixIcon: <DownOutlined />,
    onChange: (_, selectedDate) => setFilterValue('dateFilter', selectedDate)
  };

  return (
    <div className="FilterControls">
      <div className="ContactFilters">
        <Input
          placeholder={t('contacts.filters.search')}
          prefix={<SearchOutlined />}
          onChange={({ target: { value } }) => setFilterValue('textFilter', value)}
        />
        {fullFilters && [
          <Select
            allowClear
            onChange={value => setFilterValue('statusFilter', value)}
            placeholder={t('contacts.filters.status')}
          >
            {statuses.map(statusType => (
              <Select.Option key={statusType} value={statusType}>
                {t(`holders.status.${statusType}`)}
              </Select.Option>
            ))}
          </Select>,
          <CustomInputGroup prefixIcon="calendar">
            <CustomDatePicker {...datePickerProps} />
          </CustomInputGroup>,
          <CustomInputGroup
            onClick={toggleSortDirection}
            prefixIcon={isAscending ? 'sort-ascending' : 'sort-descending'}
          >
            <Select defaultValue={CONTACT_SORTING_KEYS.name} onChange={setSortingBy}>
              <Select.Option value={CONTACT_SORTING_KEYS.name}>
                {t('contacts.filters.name')}
              </Select.Option>
              <Select.Option value={CONTACT_SORTING_KEYS.createdAt}>
                {t('contacts.filters.createdAt')}
              </Select.Option>
              <Select.Option value={CONTACT_SORTING_KEYS.externalId}>
                {t('contacts.filters.externalId')}
              </Select.Option>
            </Select>
          </CustomInputGroup>
        ]}
      </div>
    </div>
  );
};

ConnectionsFilter.defaultProps = {
  searchText: undefined,
  setStatus: undefined,
  fullFilters: true
};

ConnectionsFilter.propTypes = {
  setSearchText: PropTypes.func.isRequired,
  searchText: PropTypes.string,
  fullFilters: PropTypes.bool,
  setStatus: PropTypes.func,
  sortingDirection: PropTypes.oneOf([SORTING_DIRECTIONS.ascending, SORTING_DIRECTIONS.descending])
    .isRequired,
  setSortingDirection: PropTypes.func.isRequired,
  setSortingField: PropTypes.func.isRequired,
  setCreatedAt: PropTypes.func.isRequired
};

export default ConnectionsFilter;
