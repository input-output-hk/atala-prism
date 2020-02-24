import React, { useEffect } from 'react';
import { Col, Row } from 'antd';
import PropTypes from 'prop-types';

import './_style.scss';

const SplittedPageInside = ({ onMount, renderLeft, renderRight }) => {
  useEffect(() => {
    if (onMount) onMount();
  }, [onMount]);

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
  onMount: PropTypes.func,
  renderLeft: PropTypes.func.isRequired,
  renderRight: PropTypes.func.isRequired
};

SplittedPageInside.defaultProps = {
  onMount: () => {}
};

export default SplittedPageInside;
