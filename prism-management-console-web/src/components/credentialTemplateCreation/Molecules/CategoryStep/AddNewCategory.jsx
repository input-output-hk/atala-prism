import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Col, Row } from 'antd';
import addNewCategoryIcon from '../../../../images/addNewCategoryIcon.svg';

const AddNewCategory = ({ onClick }) => {
  const { t } = useTranslation();
  return (
    <Col type="flex" align="middle" className="TypeCard shadow" onClick={onClick}>
      <Row className="header-name">{t('credentialTemplateCreation.actions.addCategory')}</Row>
      <div className="line" />
      <img className="img-credential" src={addNewCategoryIcon} alt="addNewCategoryIcon" />
    </Col>
  );
};

AddNewCategory.propTypes = {
  onClick: PropTypes.func.isRequired
};

export default AddNewCategory;
