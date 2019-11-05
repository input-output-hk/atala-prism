import React from 'react';
import { Form } from 'antd';
import PropTypes from 'prop-types';

import './_style.scss';

const GenericForm = ({ items, form: { getFieldDecorator } }) => (
  <Form>
    {items.map(({ fieldDecoratorData, label, key, className, input }) => {
      const itemProps = { label, key, className, colon: false };
      return (
        <Form.Item {...itemProps}>{getFieldDecorator(key, fieldDecoratorData)(input)}</Form.Item>
      );
    })}
  </Form>
);

GenericForm.propTypes = {
  items: PropTypes.arrayOf(PropTypes.shape()).isRequired,
  form: PropTypes.shape().isRequired
};

const CustomForm = Form.create({ name: 'custom_form' })(GenericForm);

export default CustomForm;
