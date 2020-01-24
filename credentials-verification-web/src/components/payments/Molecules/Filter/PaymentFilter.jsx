import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Col, DatePicker, Icon, Input, Row } from 'antd';

const PaymentFilter = ({ setFrom, setTo, setPayer, payer }) => {
  const { t } = useTranslation();

  const fromProps = {
    disabled: true,
    id: 'from',
    placeholder: t('payment.filterBy', { field: t('payment.fields.from') }),
    suffixIcon: <Icon type="down" />,
    onChange: (_, dateString) => setFrom(dateString)
  };

  const toProps = {
    disabled: true,
    id: 'to',
    placeholder: t('payment.filterBy', { field: t('payment.fields.to') }),
    suffixIcon: <Icon type="down" />,
    onChange: (_, dateString) => setTo(dateString)
  };

  return (
    <div className="FilterControls">
      <Row gutter={8}>
        <Col span={8}>
          <DatePicker {...fromProps} />
        </Col>
        <Col span={8}>
          <DatePicker {...toProps} />
        </Col>
        <Col span={8}>
          <Input
            disabled
            id="payer"
            placeholder={t('payment.filterBy', { field: t('payment.fields.payer') })}
            prefix={<Icon type="search" />}
            onChange={({ target: { value } }) => setPayer(value)}
            allowClear
            value={payer}
          />
        </Col>
      </Row>
    </div>
  );
};

PaymentFilter.defaultProps = {
  payer: ''
};

PaymentFilter.propTypes = {
  setFrom: PropTypes.func.isRequired,
  setTo: PropTypes.func.isRequired,
  setPayer: PropTypes.func.isRequired,
  payer: PropTypes.string
};

export default PaymentFilter;
