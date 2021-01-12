import React from 'react';
import { Popover, Button } from 'antd';

import './_style.scss';

const PopOver = ({ content }) => (
  <Popover placement="left" content={content}>
    <Button className="moreButton">...</Button>
  </Popover>
);
export default PopOver;
