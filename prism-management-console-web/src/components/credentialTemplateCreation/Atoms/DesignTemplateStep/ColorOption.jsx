import React from 'react';
import PropTypes from 'prop-types';
import { Radio } from 'antd';
import { CheckOutlined } from '@ant-design/icons';
import Avatar from 'antd/lib/avatar/avatar';
import contrast from 'get-contrast';

const ColorOption = ({ color, showCheckmark }) => {
  const contrastVsWhite = contrast.ratio('#FFFFFF', color);
  const threshold = contrast.ratio('#FFFFFF', '#D8D8D8');
  const isBrightColor = contrastVsWhite < threshold;

  const circleStyle = {
    backgroundColor: color,
    ...(isBrightColor && { border: '1px solid #D8D8D8' })
  };
  const checkmarkStyle = { fontSize: '16px', ...(isBrightColor && { color: '#000000' }) };

  return (
    <Radio value={color}>
      <Avatar
        style={circleStyle}
        icon={showCheckmark && <CheckOutlined style={checkmarkStyle} />}
      />
    </Radio>
  );
};

ColorOption.propTypes = {
  color: PropTypes.string.isRequired,
  showCheckmark: PropTypes.bool.isRequired
};

export default ColorOption;
