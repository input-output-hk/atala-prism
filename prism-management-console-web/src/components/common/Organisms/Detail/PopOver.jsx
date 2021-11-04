import React from 'react';
import PropTypes from 'prop-types';
import { Popover, Button } from 'antd';

import './_style.scss';

const PopOver = ({ content }) => (
  <Popover placement="left" content={content}>
    <Button className="moreButton">...</Button>
  </Popover>
);

PopOver.propTypes = {
  content: PropTypes.node.isRequired
};
export default PopOver;
