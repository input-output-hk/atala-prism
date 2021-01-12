import React from 'react';
import './_style.scss';

const LabelItem = ({ theme, labelText }) => {
  const classname = 'LabelItem ' + theme;

  return (
    <div className={classname}>
      <p className="LabelText">{labelText}</p>
    </div>
  );
};

export default LabelItem;
