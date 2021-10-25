import React from 'react';
import { DownOutlined, SearchOutlined } from '@ant-design/icons';
import { Input, Select } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import CustomInputGroup from '../../../common/Atoms/CustomInputGroup/CustomInputGroup';
import CustomDateRangePicker from '../../../common/Atoms/CustomDatePicker/CustomDateRangePicker';
import { GROUP_SORTING_KEYS, SORTING_DIRECTIONS } from '../../../../helpers/constants';

const GroupFilters = ({
  setName,
  setDateRange,
  setSortingKey,
  setSortingDirection,
  sortingDirection
}) => {
  const { t } = useTranslation();

  const datePickerProps = {
    placeholder: [t('groups.filters.createdAfter'), t('groups.filters.createdBefore')],
    suffixIcon: <DownOutlined />,
    onChange: (_, selectedRange) => setDateRange(selectedRange)
  };

  const isAscending = sortingDirection === SORTING_DIRECTIONS.ascending;

  return (
    <div className="FilterControls">
      <div className="w-100">
        <div>
          <Input
            placeholder={t('groups.filters.search')}
            prefix={<SearchOutlined />}
            onChange={({ target: { value } }) => setName(value)}
          />
        </div>
        {setDateRange && (
          <div>
            <CustomInputGroup prefixIcon="calendar">
              <CustomDateRangePicker {...datePickerProps} />
            </CustomInputGroup>
          </div>
        )}
        <div>
          <CustomInputGroup
            onClick={() =>
              setSortingDirection(
                isAscending ? SORTING_DIRECTIONS.descending : SORTING_DIRECTIONS.ascending
              )
            }
            prefixIcon={isAscending ? 'sort-ascending' : 'sort-descending'}
          >
            <Select defaultValue={GROUP_SORTING_KEYS.name} onChange={key => setSortingKey(key)}>
              <Select.Option value={GROUP_SORTING_KEYS.name}>
                {t('groups.filters.name')}
              </Select.Option>
              <Select.Option value={GROUP_SORTING_KEYS.createdAt}>
                {t('groups.filters.createdAt')}
              </Select.Option>
              <Select.Option value={GROUP_SORTING_KEYS.numberOfContacts}>
                {t('groups.filters.numberOfContacts')}
              </Select.Option>
            </Select>
          </CustomInputGroup>
        </div>
      </div>
    </div>
  );
};

GroupFilters.defaultProps = {
  setDateRange: null
};

GroupFilters.propTypes = {
  setName: PropTypes.func.isRequired,
  setDateRange: PropTypes.func,
  setSortingKey: PropTypes.func.isRequired,
  setSortingDirection: PropTypes.func.isRequired,
  sortingDirection: PropTypes.string.isRequired
};

export default GroupFilters;
