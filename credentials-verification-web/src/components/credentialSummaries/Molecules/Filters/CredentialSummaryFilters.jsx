import React, { useState, useEffect } from 'react';
import { DatePicker, Icon, Input, Row, Col } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';

const CredentialSummaryFilters = ({ getCredentialSummaries }) => {
  const { t } = useTranslation();

  const [date, setDate] = useState();
  const [name, setName] = useState('');

  useEffect(() => {
    getCredentialSummaries([], date, name);
  }, [date, name]);

  const datePickerProps = {
    placeholder: t('groups.filters.date'),
    suffixIcon: <Icon type="down" />,
    onChange: (_, dateString) => setDate(dateString)
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
            onChange={({ target: { value } }) => setName(value)}
          />
        </Col>
      </Row>
    </div>
  );
};

CredentialSummaryFilters.propTypes = {
  getCredentialSummaries: PropTypes.func.isRequired
};

export default CredentialSummaryFilters;
