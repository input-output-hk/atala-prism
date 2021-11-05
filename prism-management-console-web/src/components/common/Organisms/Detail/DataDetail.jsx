import React from 'react';
import PropTypes from 'prop-types';
import PopOver from './PopOver';

const DataDetail = ({ img, title, data, contentPopOver }) => (
  <div className="hashedFileContainer">
    <div style={{ display: 'flex', width: '70%' }}>
      <img src={img} alt="" />
      <div className="hashedFileContainerText">
        <span>{title}</span>
        <p>{data}</p>
      </div>
    </div>
    <PopOver content={contentPopOver} />
  </div>
);

DataDetail.propTypes = {
  img: PropTypes.node.isRequired,
  title: PropTypes.string.isRequired,
  data: PropTypes.string.isRequired,
  contentPopOver: PropTypes.node.isRequired
};

export default DataDetail;
