import React from 'react';
import PropTypes from 'prop-types';
import { Form } from 'antd';
import '@ant-design/compatible/assets/index.css';

const EditableRow = ({ EditableContext, index, ...props }) => {
  const [form] = Form.useForm();
  return (
    <Form form={form} component={false}>
      <EditableContext.Provider value={form}>
        <tr {...props} />
      </EditableContext.Provider>
    </Form>
  );
};

EditableRow.propTypes = {
  EditableContext: PropTypes.shape({ Provider: PropTypes.element }).isRequired,
  form: PropTypes.shape().isRequired
};

export default EditableRow;
