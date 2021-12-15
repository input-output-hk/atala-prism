import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { DownOutlined } from '@ant-design/icons';
import { Select } from 'antd';
import CustomInputGroup from '../../../common/Atoms/CustomInputGroup/CustomInputGroup';
import CustomDatePicker from '../../../common/Atoms/CustomDatePicker/CustomDatePicker';
import {
  CONNECTED,
  CONTACT_SORTING_KEYS,
  PENDING_CONNECTION,
  SORTING_DIRECTIONS
} from '../../../../helpers/constants';

const ContactAdditionalFiltersMenu = ({
  sortDirection,
  setSortingBy,
  setFilterValue,
  toggleSortDirection
}) => {
  const { t } = useTranslation();

  const statuses = [PENDING_CONNECTION, CONNECTED];
  const isAscending = sortDirection === SORTING_DIRECTIONS.ascending;

  const datePickerProps = {
    placeholder: t('contacts.filters.createdAt'),
    suffixIcon: <DownOutlined />,
    onChange: (_, selectedDate) => setFilterValue('dateFilter', selectedDate)
  };

  return (
    <div className="FiltersMenu">
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
      </Select>

      <CustomInputGroup prefixIcon="calendar">
        <CustomDatePicker {...datePickerProps} />
      </CustomInputGroup>

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
    </div>
  );
};

ContactAdditionalFiltersMenu.propTypes = {
  sortDirection: PropTypes.string.isRequired,
  setSortingBy: PropTypes.func.isRequired,
  setFilterValue: PropTypes.func.isRequired,
  toggleSortDirection: PropTypes.func.isRequired
};

export default ContactAdditionalFiltersMenu;
