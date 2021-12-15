import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { DownOutlined } from '@ant-design/icons';
import { Select } from 'antd';
import CustomInputGroup from '../../../common/Atoms/CustomInputGroup/CustomInputGroup';
import CustomDatePicker from '../../../common/Atoms/CustomDatePicker/CustomDatePicker';
import { CONNECTED, PENDING_CONNECTION } from '../../../../helpers/constants';

const ContactAdditionalFiltersMenu = ({ setFilterValue }) => {
  const { t } = useTranslation();

  const statuses = [PENDING_CONNECTION, CONNECTED];

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
    </div>
  );
};

ContactAdditionalFiltersMenu.propTypes = {
  setFilterValue: PropTypes.func.isRequired
};

export default ContactAdditionalFiltersMenu;
