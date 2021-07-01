import React from 'react';
import { Col, Row } from 'antd';
import HeaderEditor from '../../Atoms/DesignTemplateStep/HeaderEditor';
import BodyEditor from '../../Atoms/DesignTemplateStep/BodyEditor';

const ContentSettings = props => (
  <Col>
    <Row>
      <HeaderEditor />
    </Row>
    <Row>
      <BodyEditor />
    </Row>
  </Col>
);

ContentSettings.propTypes = {};

export default ContentSettings;
