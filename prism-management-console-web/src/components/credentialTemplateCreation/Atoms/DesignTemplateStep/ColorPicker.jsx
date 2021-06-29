import React from 'react';
import PropTypes from 'prop-types';
import { Form, Radio } from 'antd';
import { CheckOutlined } from '@ant-design/icons';
import Avatar from 'antd/lib/avatar/avatar';

const ColorPicker = ({ colors, selected, ...formItemProps }) => (
  <Form.Item
    {...formItemProps}
    rules={[
      {
        required: true
      }
    ]}
  >
    <Radio.Group>
      {colors.map(c => (
        <Radio value={c}>
          <Avatar style={{ backgroundColor: c }} icon={selected === c && <CheckOutlined />} />
        </Radio>
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
