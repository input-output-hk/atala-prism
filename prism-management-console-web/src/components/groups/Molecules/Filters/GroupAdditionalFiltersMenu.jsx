import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { DownOutlined } from '@ant-design/icons';
import moment from 'moment';
import CustomInputGroup from '../../../common/Atoms/CustomInputGroup/CustomInputGroup';
import CustomDatePicker from '../../../common/Atoms/CustomDatePicker/CustomDatePicker';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import { DEFAULT_DATE_FORMAT } from '../../../../helpers/constants';
import './_style.scss';

const GroupAdditionalFiltersMenu = ({ dateFilter, setFilterValue, resetFilters }) => {
  const { t } = useTranslation();

  const datePickerProps = {
    value: dateFilter && moment(dateFilter, DEFAULT_DATE_FORMAT),
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
        <div className="ClearFiltersButton">
          <CustomButton
            buttonProps={{
              onClick: resetFilters,
              className: 'theme-link'
            }}
            buttonText={t('actions.clear')}
          />
        </div>
      </div>
    </div>
  );
};

GroupAdditionalFiltersMenu.propTypes = {
  dateFilter: PropTypes.string.isRequired,
  setFilterValue: PropTypes.func.isRequired,
  resetFilters: PropTypes.func.isRequired
};

export default GroupAdditionalFiltersMenu;
