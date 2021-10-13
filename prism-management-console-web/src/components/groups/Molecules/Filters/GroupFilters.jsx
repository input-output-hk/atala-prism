import React from 'react';
import { DownOutlined, SearchOutlined } from '@ant-design/icons';
import { Input, Select } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { observer } from 'mobx-react-lite';
import CustomInputGroup from '../../../common/Atoms/CustomInputGroup/CustomInputGroup';
import CustomDateRangePicker from '../../../common/Atoms/CustomDatePicker/CustomDateRangePicker';
import { GROUP_SORTING_KEYS, SORTING_DIRECTIONS } from '../../../../helpers/constants';
import { useGroupUiState } from '../../../../hooks/useGroupStore';
import './_style.scss';

const GroupFilters = observer(({ fullFilters }) => {
  const { t } = useTranslation();

  const { sortingDirection, setSortingBy, setFilterValue, toggleSortDirection } = useGroupUiState();

  const datePickerProps = {
    placeholder: [t('groups.filters.createdAfter'), t('groups.filters.createdBefore')],
    suffixIcon: <DownOutlined />,
    onChange: (_, selectedRange) => setFilterValue('dateFilter', selectedRange || [])
  };

  const isAscending = sortingDirection === SORTING_DIRECTIONS.ascending;

  return (
    <div className="FilterControls">
      <div className="w-100">
        <div>
          <Input
            placeholder={t('groups.filters.search')}
            prefix={<SearchOutlined />}
            onChange={({ target: { value } }) => setFilterValue('nameFilter', value)}
          />
        </div>
        {fullFilters && (
          <>
            <div>
              <CustomInputGroup prefixIcon="calendar">
                <CustomDateRangePicker {...datePickerProps} />
              </CustomInputGroup>
            </div>
            <div>
              <CustomInputGroup
                onClick={toggleSortDirection}
                prefixIcon={isAscending ? 'sort-ascending' : 'sort-descending'}
              >
                <Select defaultValue={GROUP_SORTING_KEYS.name} onChange={setSortingBy}>
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
          </>
        )}
      </div>
    </div>
  );
});

GroupFilters.defaultProps = {
  fullFilters: false
};

GroupFilters.propTypes = {
  fullFilters: PropTypes.bool
};

export default GroupFilters;
