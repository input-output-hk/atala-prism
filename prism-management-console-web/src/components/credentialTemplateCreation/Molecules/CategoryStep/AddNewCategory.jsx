import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Col, Row } from 'antd';

const AddNewCategory = ({ onClick }) => {
  const { t } = useTranslation();
  return (
    <Col type="flex" align="middle" className="TypeCard2 shadow" onClick={onClick}>
      <Row className="header-name">{t('credentialTemplateCreation.actions.addCategory')}</Row>
    </Col>
  );
};

AddNewCategory.propTypes = {
  onClick: PropTypes.func.isRequired
};

export default AddNewCategory;
