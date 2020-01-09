import React from 'react';
import { Col, Row } from 'antd';
import PropTypes from 'prop-types';

import './_style.scss';

const SplittedPage = ({ renderLeft, renderRight }) => {
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
  redirector: PropTypes.shape({ redirectToHome: PropTypes.func }).isRequired,
  renderLeft: PropTypes.func.isRequired,
  renderRight: PropTypes.func.isRequired
};

export default SplittedPage;
