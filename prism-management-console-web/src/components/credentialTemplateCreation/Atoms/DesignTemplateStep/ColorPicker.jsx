import React from 'react';
import PropTypes from 'prop-types';
import { Form, Radio } from 'antd';
import ColorOption from './ColorOption';

const ColorPicker = ({ colors, selected, ...formItemProps }) => (
  <Form.Item {...formItemProps} rules={[{ required: true }]}>
    <Radio.Group>
      {colors.map(c => (
        <ColorOption color={c} showCheckmark={c === selected} />
      ))}
    </Radio.Group>
  </Form.Item>
);

ColorPicker.defaultProps = {
  colors: []
};

ColorPicker.propTypes = {
  colors: PropTypes.arrayOf(PropTypes.string),
  selected: PropTypes.string.isRequired
};

export default ColorPicker;
