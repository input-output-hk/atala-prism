import React from 'react';
import { useTranslation } from 'react-i18next';
import { Col, Row } from 'antd';
import addNewCategoryIcon from '../../../images/addNewCategoryIcon.svg';

const AddNewCategory = () => {
  const { t } = useTranslation();
  return (
    <Col type="flex" align="middle" className="TypeCard">
      <Row className="header-name">{t('credentialTemplateCreation.actions.addCategory')}</Row>
      <div className="line" />
      <img className="img-credential" src={addNewCategoryIcon} alt="addNewCategoryIcon" />
    </Col>
  );
};

export default AddNewCategory;
