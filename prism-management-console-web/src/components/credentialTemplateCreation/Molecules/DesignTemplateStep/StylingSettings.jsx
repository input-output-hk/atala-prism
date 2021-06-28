import React from 'react';
import { Col, Row } from 'antd';
import LayoutSelector from '../../Atoms/DesignTemplateStep/LayoutSelector';

const ThemeOptions = () => <div>{'<ThemeOptions/>'}</div>;
const TemplateIcons = () => <div>{'<TemplateIcons/>'}</div>;

const StylingSettings = props => (
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
