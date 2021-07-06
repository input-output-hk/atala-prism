import React from 'react';
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
export default DataDetail;
