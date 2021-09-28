import React from 'react';
import PropTypes from 'prop-types';
import { Col, Row } from 'antd';
import { credentialTypeShape } from '../../../../helpers/propShapes';
import defaultLogo from '../../../../images/templates/genericUserIcon.svg';
import './_style.scss';

const TypeCard = ({ typeKey, credentialType, isSelected, onClick, logo }) => (
  <Col
    type="flex"
    align="middle"
    className={`TypeCard shadow ${isSelected ? 'selected' : ''}`}
    onClick={() => onClick(typeKey)}
  >
    <Row className="header-name">
      <img className="img-logo" src={logo || defaultLogo} alt={`${typeKey}-logo`} />
      {credentialType.name}
    </Row>
  </Col>
);

TypeCard.propTypes = {
  typeKey: PropTypes.string.isRequired,
  credentialType: credentialTypeShape.isRequired,
  isSelected: PropTypes.bool.isRequired,
  onClick: PropTypes.func.isRequired,
  logo: PropTypes.string.isRequired
};

export default TypeCard;
