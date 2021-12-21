import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { DownOutlined } from '@ant-design/icons';
import { Select } from 'antd';
import moment from 'moment';
import CustomInputGroup from '../../../common/Atoms/CustomInputGroup/CustomInputGroup';
import CustomDatePicker from '../../../common/Atoms/CustomDatePicker/CustomDatePicker';
import { CONNECTED, DEFAULT_DATE_FORMAT, PENDING_CONNECTION } from '../../../../helpers/constants';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import './_style.scss';

const ContactAdditionalFiltersMenu = ({
  statusFilter,
  dateFilter,
  setFilterValue,
  resetAdditionalFilters
}) => {
  const { t } = useTranslation();

  const statuses = [PENDING_CONNECTION, CONNECTED];

  const datePickerProps = {
    value: dateFilter && moment(dateFilter, DEFAULT_DATE_FORMAT),
    placeholder: t('contacts.filters.createdAt'),
    suffixIcon: <DownOutlined />,
    onChange: (_, selectedDate) => setFilterValue('dateFilter', selectedDate)
  };

  return (
    <div className="FiltersMenu">
      <div className="selectLabel">
        <p>{t('actions.filterBy', { column: t('contacts.filters.status') })}</p>
        <Select
          allowClear
          value={statusFilter}
          onChange={value => setFilterValue('statusFilter', value)}
          placeholder={t('contacts.filters.status')}
        >
          {statuses.map(statusType => (
            <Select.Option key={statusType} value={statusType}>
              {t(`holders.status.${statusType}`)}
            </Select.Option>
          ))}
        </Select>
        <hr className="FilterDivider" />
        <p>{t('actions.filterBy', { column: t('contacts.filters.createdAt') })}</p>
        <CustomInputGroup prefixIcon="calendar">
          <CustomDatePicker {...datePickerProps} />
        </CustomInputGroup>
        <div className="ClearFiltersButton">
          <CustomButton
            buttonProps={{
              onClick: resetAdditionalFilters,
              className: 'theme-link'
            }}
            buttonText={t('actions.clear')}
          />
        </div>
      </div>
    </div>
  );
};

ContactAdditionalFiltersMenu.propTypes = {
  statusFilter: PropTypes.string.isRequired,
  dateFilter: PropTypes.string.isRequired,
  setFilterValue: PropTypes.func.isRequired,
  resetAdditionalFilters: PropTypes.func.isRequired
};

export default ContactAdditionalFiltersMenu;
