import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { DownOutlined } from '@ant-design/icons';
import CustomInputGroup from '../../../common/Atoms/CustomInputGroup/CustomInputGroup';
import CustomDatePicker from '../../../common/Atoms/CustomDatePicker/CustomDatePicker';

const GroupAdditionalFiltersMenu = ({ setFilterValue }) => {
  const { t } = useTranslation();

  const datePickerProps = {
    placeholder: t('contacts.filters.createdAt'),
    suffixIcon: <DownOutlined />,
    onChange: (_, selectedDate) => setFilterValue('dateFilter', selectedDate)
  };

  return (
    <div className="FiltersMenu">
      <CustomInputGroup prefixIcon="calendar">
        <CustomDatePicker {...datePickerProps} />
      </CustomInputGroup>
    </div>
  );
};

GroupAdditionalFiltersMenu.propTypes = {
  setFilterValue: PropTypes.func.isRequired
};

export default GroupAdditionalFiltersMenu;
