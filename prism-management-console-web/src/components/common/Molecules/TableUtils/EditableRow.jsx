import React from 'react';
import PropTypes from 'prop-types';
import { Form } from 'antd';

const EditableRow = ({ EditableContext: { Provider }, form, ...props }) => (
  <Provider value={form}>
    <tr {...props} />
  </Provider>
);

EditableRow.propTypes = {
  EditableContext: PropTypes.shape({ Provider: PropTypes.element }).isRequired,
  form: PropTypes.shape().isRequired
};

export default Form.create()(EditableRow);
