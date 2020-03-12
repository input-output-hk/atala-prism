import React from 'react';
import { Col, Row } from 'antd';
import PropTypes from 'prop-types';

import './_style.scss';

const SplittedPageInside = ({ renderLeft, renderRight }) => {
  return (
    <div className="SplittedPageInsideContent">
      <Row>
        <Col xs={24} lg={14}>
          {renderLeft()}
        </Col>
        <Col xs={24} lg={10} className="RightSide">
          {renderRight()}
        </Col>
      </Row>
    </div>
  );
};

SplittedPageInside.propTypes = {
  renderLeft: PropTypes.func.isRequired,
  renderRight: PropTypes.func.isRequired
};

export default SplittedPageInside;
