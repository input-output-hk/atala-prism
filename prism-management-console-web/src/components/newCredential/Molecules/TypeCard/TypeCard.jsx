import React from 'react';
import PropTypes from 'prop-types';
import { Col, Row } from 'antd';

import './_style.scss';

const TypeCard = ({ typeKey, credentialType, isSelected, onClick }) => (
  <Col
    type="flex"
    align="middle"
    className={`TypeCard shadow ${isSelected ? 'selected' : ''}`}
    onClick={() => onClick(typeKey)}
  >
    <Row className="header-name">
      <img className="img-logo" src={credentialType.logo} alt={`${typeKey}-logo`} />
      {credentialType.name}
    </Row>
    <div className="line" />
    <img className="img-credential" src={credentialType.sampleImage} alt={`${typeKey}-sample`} />
  </Col>
);

TypeCard.defaultProps = {};

TypeCard.propTypes = {
  typeKey: PropTypes.string.isRequired,
  credentialType: PropTypes.shape({
    name: PropTypes.string.isRequired,
    logo: PropTypes.string.isRequired,
    sampleImage: PropTypes.string.isRequired
  }).isRequired,
  isSelected: PropTypes.bool.isRequired,
  onClick: PropTypes.func.isRequired
};

export default TypeCard;
