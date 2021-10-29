import React from 'react';
import PropTypes from 'prop-types';
import { Form, Radio } from 'antd';
import ColorOption from './ColorOption';
import { themeColors } from '../../../../helpers/colors';

const ColorPicker = ({ colors, selected, ...formItemProps }) => (
  <Form.Item {...formItemProps} rules={[{ required: true }]}>
    <Radio.Group>
      {colors.map(c => (
        <ColorOption key={c} color={c} showCheckmark={c === selected} />
      ))}
    </Radio.Group>
  </Form.Item>
);

ColorPicker.defaultProps = {
  colors: themeColors
};

ColorPicker.propTypes = {
  colors: PropTypes.arrayOf(PropTypes.string),
  selected: PropTypes.string.isRequired
};

export default ColorPicker;
