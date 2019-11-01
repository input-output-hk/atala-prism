import React from 'react';
import { DatePicker, Icon, Input, Row, Col } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';

const ConnectionFilters = ({ changeDate, changeFilter }) => {
  const { t } = useTranslation();

  const datePickerProps = {
    placeholder: t('groups.filters.date'),
    suffixIcon: <Icon type="down" />,
    onChange: (_, dateString) => changeDate(dateString)
  };

  return (
    <div className="FilterControls">
      <Row gutter={16}>
        <Col span={12}>
          <DatePicker {...datePickerProps} />
        </Col>
        <Col span={12}>
          <Input
            placeholder={t('groups.filters.search')}
            prefix={<Icon type="search" />}
            onChange={({ target: { value } }) => changeFilter(value)}
          />
        </Col>
      </Row>
    </div>
  );
};

ConnectionFilters.propTypes = {
  changeDate: PropTypes.func.isRequired,
  changeFilter: PropTypes.func.isRequired
};

export default ConnectionFilters;
