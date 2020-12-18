import React from 'react';
import './_style.scss';
import { Icon } from 'antd';

const CustomInputGroup = ({ prefixIcon, children }) => {
  const { disabled } = children?.props;
  const containerClass = `InputContainer ${disabled ? 'ant-input-disabled' : ''}`;
  return (
    <div className={containerClass}>
      <span className="InputPrefix">
        <Icon type={prefixIcon} />
      </span>
      <span className="ChildrenContainer">{children}</span>
    </div>
  );
};

export default CustomInputGroup;
