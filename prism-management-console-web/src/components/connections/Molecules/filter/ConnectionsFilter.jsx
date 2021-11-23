import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { DownOutlined, SearchOutlined } from '@ant-design/icons';
import { Input, Select } from 'antd';
import { observer } from 'mobx-react-lite';
import {
  PENDING_CONNECTION,
  CONNECTED,
  SORTING_DIRECTIONS,
  CONTACT_SORTING_KEYS
} from '../../../../helpers/constants';
import CustomInputGroup from '../../../common/Atoms/CustomInputGroup/CustomInputGroup';
import CustomDatePicker from '../../../common/Atoms/CustomDatePicker/CustomDatePicker';
import './_style.scss';

const ConnectionsFilter = observer(({ filterSortingProps, showFullFilter, localStateFilter }) => {
  const { t } = useTranslation();
  const {
    sortDirection,
    setSortingBy,
    setFilterValue: setGlobalFilterValue,
    toggleSortDirection
  } = filterSortingProps;

  const setFilterValue = localStateFilter ? localStateFilter.setValue : setGlobalFilterValue;

  const statuses = [PENDING_CONNECTION, CONNECTED];
  const isAscending = sortDirection === SORTING_DIRECTIONS.ascending;

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
        {showFullFilter && [
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
});

ConnectionsFilter.defaultProps = {
  filterSortingProps: {},
  showFullFilter: true,
  localStateFilter: null
};

ConnectionsFilter.propTypes = {
  filterSortingProps: PropTypes.shape({
    sortDirection: PropTypes.string,
    setSortingBy: PropTypes.func,
    setFilterValue: PropTypes.func,
    toggleSortDirection: PropTypes.func
  }),
  showFullFilter: PropTypes.bool,
  localStateFilter: PropTypes.shape({
    setValue: PropTypes.func
  })
};

export default ConnectionsFilter;
