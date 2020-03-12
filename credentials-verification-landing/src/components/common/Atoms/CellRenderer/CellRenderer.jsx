import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';

import './_style.scss';

const CellRenderer = ({ title, value, firstValue, componentName }) => {
  const { t } = useTranslation();
  return (
    <div className="CellRenderer">
      <p className="TableLabel">{t(`${componentName}.table.columns.${title}`)}</p>
      <p className="TableText">{value}</p>
      <p className="TableTextBold">{firstValue}</p>
    </div>
  );
};

CellRenderer.propTypes = {
  title: PropTypes.string.isRequired,
  firstValue: PropTypes.string.isRequired,
  value: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
  componentName: PropTypes.string.isRequired
};

export default CellRenderer;
