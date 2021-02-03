import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { DownOutlined, SearchOutlined } from '@ant-design/icons';
import { Col, Input, Row } from 'antd';
import CustomDatePicker from '../../../common/Atoms/CustomDatePicker/CustomDatePicker';

const PaymentFilter = ({ setFrom, setTo, setPayer, payer }) => {
  const { t } = useTranslation();

  const fromProps = {
    disabled: true,
    id: 'from',
    placeholder: t('payment.filterBy', { field: t('payment.fields.from') }),
    suffixIcon: <DownOutlined />,
    onChange: (_, dateString) => setFrom(dateString)
  };

  const toProps = {
    disabled: true,
    id: 'to',
    placeholder: t('payment.filterBy', { field: t('payment.fields.to') }),
    suffixIcon: <DownOutlined />,
    onChange: (_, dateString) => setTo(dateString)
  };

  return (
    <div className="FilterControls">
      <Row gutter={8}>
        <Col span={8}>
          <CustomDatePicker {...fromProps} />
        </Col>
        <Col span={8}>
          <CustomDatePicker {...toProps} />
        </Col>
        <Col span={8}>
          <Input
            disabled
            id="payer"
            placeholder={t('payment.filterBy', { field: t('payment.fields.payer') })}
            prefix={<SearchOutlined />}
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
