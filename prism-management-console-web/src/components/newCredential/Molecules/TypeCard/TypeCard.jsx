import React from 'react';
import PropTypes from 'prop-types';
import { Col, Row } from 'antd';
import { credentialTypeShape } from '../../../../helpers/propShapes';
import defaultLogo from '../../../../images/templates/genericUserIcon.svg';
import defaultSampleImage from '../../../../images/TemplateLayout_0.svg';
import './_style.scss';

const TypeCard = ({ typeKey, credentialType, isSelected, onClick, logo, sampleImage }) => (
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
    <div className="line" />
    <img
      className={`template-sample-image ${sampleImage ? '' : 'genericImage'}`}
      src={sampleImage || defaultSampleImage}
      alt={`${typeKey}-sample`}
    />
  </Col>
);

TypeCard.defaultProps = {
  sampleImage: undefined
};

TypeCard.propTypes = {
  typeKey: PropTypes.string.isRequired,
  credentialType: credentialTypeShape.isRequired,
  isSelected: PropTypes.bool.isRequired,
  onClick: PropTypes.func.isRequired,
  logo: PropTypes.string.isRequired,
  sampleImage: PropTypes.string
};

export default TypeCard;
