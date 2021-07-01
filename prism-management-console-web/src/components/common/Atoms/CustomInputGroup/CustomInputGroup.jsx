import React from 'react';
import { Icon as LegacyIcon } from '@ant-design/compatible';

import './_style.scss';

const CustomInputGroup = ({ prefixIcon, children, onClick }) => {
  const { disabled } = children?.props;
  const containerClass = `InputContainer ${disabled ? 'ant-input-disabled' : ''}`;
  return (
    <div className={containerClass}>
      <span className="InputPrefix">
        <LegacyIcon type={prefixIcon} onClick={onClick} />
      </span>
      <span className="ChildrenContainer">{children}</span>
    </div>
  );
};

export default CustomInputGroup;
