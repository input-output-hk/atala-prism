import React, { useEffect } from 'react';
import { Col, Row } from 'antd';
import PropTypes from 'prop-types';

import './_style.scss';

const SplittedPage = ({ onMount, renderLeft, renderRight }) => {
  useEffect(() => {
    if (onMount) onMount();
  }, []);

  return (
    <div className="SplittedPageContent">
      <Row>
        <Col xs={24} lg={12}>
          {renderLeft()}
        </Col>
        <Col xs={24} lg={12}>
          {renderRight()}
        </Col>
      </Row>
    </div>
  );
};

SplittedPage.propTypes = {
  onMount: PropTypes.func.isRequired,
  renderLeft: PropTypes.func.isRequired,
  renderRight: PropTypes.func.isRequired
};

export default SplittedPage;
