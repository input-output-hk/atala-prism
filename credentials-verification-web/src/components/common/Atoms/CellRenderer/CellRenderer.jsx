import React from 'react';
import { useTranslation } from 'react-i18next';
import { Row, Col } from 'antd';
import PropTypes from 'prop-types';

import './_style.scss';

const CellRenderer = ({ title, value, componentName }) => {
  const { t } = useTranslation();

  return (
    <Row>
      <Col>
        <p className="TableLabel">{t(`${componentName}.table.columns.${title}`)}</p>
      </Col>
      <Col>
        <p className="TableText">{value}</p>
      </Col>
    </Row>
  );
};

CellRenderer.propTypes = {
  title: PropTypes.string.isRequired,
  value: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
  componentName: PropTypes.string.isRequired
};

export default CellRenderer;
