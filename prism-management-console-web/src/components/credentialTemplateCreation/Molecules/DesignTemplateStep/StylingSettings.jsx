import React from 'react';
import { Col, Row } from 'antd';
import LayoutSelector from '../../Atoms/DesignTemplateStep/LayoutSelector';
import ThemeOptions from '../../Atoms/DesignTemplateStep/ThemeOptions';
import TemplateIcons from '../../Atoms/DesignTemplateStep/TemplateIcons';

const StylingSettings = () => (
  <Col>
    <Row>
      <LayoutSelector />
    </Row>
    <Row>
      <Col>
        <ThemeOptions />
      </Col>
      <Col>
        <TemplateIcons />
      </Col>
    </Row>
  </Col>
);

StylingSettings.propTypes = {};

export default StylingSettings;
