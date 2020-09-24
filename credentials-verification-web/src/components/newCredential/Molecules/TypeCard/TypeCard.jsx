import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Col, Row } from 'antd';

import './_style.scss';

const TypeCard = ({ typeKey, credentialType, isSelected, onClick }) => {
  const { t } = useTranslation();
  return (
    <Col
      type="flex"
      align="middle"
      className={isSelected ? 'SelectedTypeCard' : 'TypeCard'}
      onClick={() => onClick(typeKey)}
    >
      <Row className="header-name">
        <img className="img-logo" src={credentialType.logo} alt={`${typeKey}-logo`} />
        {t(credentialType.name)}
      </Row>
      <div className="line" />
      <img className="img-credential" src={credentialType.sampleImage} alt={`${typeKey}-sample`} />
    </Col>
  );
};

TypeCard.defaultProps = {};

TypeCard.propTypes = {
  typeKey: PropTypes.string.isRequired,
  credentialType: PropTypes.shape({
    name: PropTypes.string.isRequired,
    logo: PropTypes.string.isRequired,
    sample: PropTypes.string.isRequired
  }).isRequired,
  isSelected: PropTypes.bool.isRequired,
  onClick: PropTypes.func.isRequired
};

export default TypeCard;
