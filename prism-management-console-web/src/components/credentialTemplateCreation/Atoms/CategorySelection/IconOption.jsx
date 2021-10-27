import React from 'react';
import PropTypes from 'prop-types';
import { Radio } from 'antd';
import './_style.scss';

const IconOption = ({ icon, selected }) => (
  <Radio value={icon}>
    <img
      className={`IconOption ${selected ? 'selected' : ''}`}
      src={icon.isCustomIcon ? icon.file.thumbUrl : icon.src}
      alt="template icon"
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
