import React, { useEffect, useState } from 'react';
import { DatePicker, Icon, Input, Row, Col, Select } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import CustomInputGroup from '../../../common/Atoms/CustomInputGroup/CustomInputGroup';
import { DEFAULT_DATE_FORMAT } from '../../../../helpers/constants';

const { Option } = Select;

const GroupFilters = ({ updateGroups }) => {
  const { t } = useTranslation();

  const [date, setDate] = useState();
  const [name, setName] = useState('');

  useEffect(() => {
    updateGroups([], date, name);
  }, [date, name]);

  const datePickerProps = {
    placeholder: t('groups.filters.date'),
    suffixIcon: <Icon type="down" />,
    format: DEFAULT_DATE_FORMAT,
    onChange: (_, dateString) => setDate(dateString)
  };

  return (
    <div className="FilterControls">
      <Row gutter={16} className="w-100">
        <Col span={4}>
          <Input
            placeholder={t('groups.filters.search')}
            prefix={<Icon type="search" />}
            onChange={({ target: { value } }) => setName(value)}
          />
        </Col>
        <Col span={4}>
          <CustomInputGroup prefixIcon="calendar">
            <DatePicker {...datePickerProps} />
          </CustomInputGroup>
        </Col>
        <Col span={4}>
          <CustomInputGroup prefixIcon="sort-ascending">
            <Select defaultValue="name" disabled>
              <Option value="name">{t('groups.filters.name')}</Option>
            </Select>
          </CustomInputGroup>
        </Col>
      </Row>
    </div>
  );
};

GroupFilters.propTypes = {
  updateGroups: PropTypes.func.isRequired
};

export default GroupFilters;
