import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Col, Row } from 'antd';

const CategoryCard = ({ category, typeKey, isSelected }) => {
  const { t } = useTranslation();
  return (
    <Col type="flex" align="middle" className={`TypeCard2 shadow ${isSelected ? 'selected' : ''}`}>
      <Row className="header-name">
        <img className="img-logo" src={category.logo} alt={`${typeKey}-logo`} />
        {t(category.name)}
      </Row>
    </Col>
  );
};

CategoryCard.propTypes = {
  category: PropTypes.shape({ name: PropTypes.string, logo: PropTypes.string }).isRequired,
  typeKey: PropTypes.string.isRequired,
  isSelected: PropTypes.bool.isRequired
};

export default CategoryCard;
