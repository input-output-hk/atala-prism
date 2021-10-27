import React, { useEffect } from 'react';
import { Col, Row } from 'antd';
import PropTypes from 'prop-types';

import './_style.scss';

const SplittedPage = ({ onMount, renderLeft, renderRight }) => {
  useEffect(() => {
    if (onMount) onMount();
  }, [onMount]);

  return (
    <div className="SplittedPageContent">
      <Row>
        <Col xs={24} lg={10}>
          {renderLeft()}
        </Col>
        <Col xs={24} lg={14}>
          {renderRight()}
        </Col>
      </Row>
    </div>
  );
};

SplittedPage.propTypes = {
  onMount: PropTypes.func,
  renderLeft: PropTypes.func.isRequired,
  renderRight: PropTypes.func.isRequired
};

SplittedPage.defaultProps = {
  onMount: null
};

export default SplittedPage;
