import React from 'react';
import PropTypes from 'prop-types';
import { Radio } from 'antd';
import './_style.scss';

const IconOption = ({ icon, selected }) => (
    <Radio value={icon.uid}>
      <img
        className={`CategoryIconOption ${selected ? 'selected' : ''}`}
        src={icon.thumbUrl}
        alt="categoryIcon"
      />
    </Radio>
);

IconOption.propTypes = {
  icon: PropTypes.string.isRequired,
  selected: PropTypes.bool.isRequired
};

export default IconOption;
