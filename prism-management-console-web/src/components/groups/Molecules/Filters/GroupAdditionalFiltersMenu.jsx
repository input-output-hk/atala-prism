import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { DownOutlined } from '@ant-design/icons';
import CustomInputGroup from '../../../common/Atoms/CustomInputGroup/CustomInputGroup';
import CustomDatePicker from '../../../common/Atoms/CustomDatePicker/CustomDatePicker';
import './_style.scss';

const GroupAdditionalFiltersMenu = ({ setFilterValue }) => {
  const { t } = useTranslation();

  const datePickerProps = {
    placeholder: t('groups.filters.createdAt'),
    suffixIcon: <DownOutlined />,
    onChange: (_, selectedDate) => setFilterValue('dateFilter', selectedDate)
  };

  return (
    <div className="FiltersMenu">
      <div className="selectLabel">
        <p>{t('actions.filterBy', { column: t('groups.filters.createdAt') })}</p>
        <CustomInputGroup prefixIcon="calendar">
          <CustomDatePicker {...datePickerProps} />
        </CustomInputGroup>
        {/* TODO: add clear all filters button here */}
      </div>
    </div>
  );
};

GroupAdditionalFiltersMenu.propTypes = {
  setFilterValue: PropTypes.func.isRequired
};

export default GroupAdditionalFiltersMenu;
