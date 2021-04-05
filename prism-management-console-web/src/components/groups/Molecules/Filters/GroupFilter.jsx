import React, { useEffect, useState } from 'react';
import { DownOutlined, SearchOutlined } from '@ant-design/icons';
import { Input, Row, Col } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import CustomInputGroup from '../../../common/Atoms/CustomInputGroup/CustomInputGroup';
import CustomDatePicker from '../../../common/Atoms/CustomDatePicker/CustomDatePicker';

const GroupFilters = ({ updateGroups }) => {
  const { t } = useTranslation();

  const [date, setDate] = useState();
  const [name, setName] = useState('');

  useEffect(() => {
    updateGroups([], date, name);
  }, [date, name, updateGroups]);

  const datePickerProps = {
    placeholder: t('groups.filters.date'),
    suffixIcon: <DownOutlined />,
    onChange: (_, dateString) => setDate(dateString)
  };

  return (
    <div className="FilterControls">
      <Row gutter={16} className="w-100">
        <Col span={4}>
          <Input
            placeholder={t('groups.filters.search')}
            prefix={<SearchOutlined />}
            onChange={({ target: { value } }) => setName(value)}
          />
        </Col>
        <Col span={4}>
          <CustomInputGroup prefixIcon="calendar">
            <CustomDatePicker {...datePickerProps} />
          </CustomInputGroup>
        </Col>
        {/* TODO: in 0.4 this will help */}
        {/* <Col span={4}>
          <CustomInputGroup prefixIcon="sort-ascending">
            <Select defaultValue="name" disabled>
              <Option value="name">{t('groups.filters.name')}</Option>
            </Select>
          </CustomInputGroup>
        </Col> */}
      </Row>
    </div>
  );
};

GroupFilters.propTypes = {
  updateGroups: PropTypes.func.isRequired
};

export default GroupFilters;
