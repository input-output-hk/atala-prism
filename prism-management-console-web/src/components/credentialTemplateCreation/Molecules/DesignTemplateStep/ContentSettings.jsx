import React from 'react';
import { Col, Row } from 'antd';

const HeaderEditor = () => <div>{'<HeaderEditor/>'}</div>;
const BodyEditor = () => <div>{'<BodyEditor/>'}</div>;

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
