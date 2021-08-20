import React from 'react';
import PropTypes from 'prop-types';
import { Radio } from 'antd';
import './_style.scss';

const IconOption = ({ icon, selected }) => (
  <Radio value={icon}>
    <img
      className={`CategoryIconOption ${selected ? 'selected' : ''}`}
      src={icon.thumbUrl}
      alt="categoryIcon"
    />
  </Radio>
);

IconOption.propTypes = {
  icon: PropTypes.shape({
    uid: PropTypes.string,
    thumbUrl: PropTypes.string
  }).isRequired,
  selected: PropTypes.bool.isRequired
};

export default IconOption;
