import React from 'react';
import { Popover, Button } from 'antd';

const PopOver = ({ content }) => (
  <Popover placement="bottom" content={content}>
    <Button className="moreButton">...</Button>
  </Popover>
);
export default PopOver;
