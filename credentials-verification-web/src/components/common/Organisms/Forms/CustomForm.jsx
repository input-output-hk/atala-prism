import React from 'react';
import { /* Button, */ Form } from 'antd';

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

const CustomForm = Form.create({ name: 'custom_form' })(GenericForm);

export default CustomForm;
