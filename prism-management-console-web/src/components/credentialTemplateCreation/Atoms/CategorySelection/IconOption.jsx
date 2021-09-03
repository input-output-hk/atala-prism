import React from 'react';
import PropTypes from 'prop-types';
import { Radio } from 'antd';
import './_style.scss';

const IconOption = ({ icon, selected }) => (
  <Radio value={icon}>
    <img
      className={`CategoryIconOption ${selected ? 'selected' : ''}`}
      src={icon.isCustomIcon ? icon.file.thumbUrl : icon.src}
      alt="categoryIcon"
    />
  </Radio>
);

IconOption.propTypes = {
  icon: PropTypes.oneOfType([
    PropTypes.shape({
      isCustomIcon: PropTypes.bool,
      src: PropTypes.string
    }),
    PropTypes.shape({
      isCustomIcon: PropTypes.bool,
      file: PropTypes.shape({ thumbUrl: PropTypes.string })
    })
  ]).isRequired,
  selected: PropTypes.bool.isRequired
};

export default IconOption;
