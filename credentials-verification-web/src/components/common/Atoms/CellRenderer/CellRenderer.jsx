import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';

import './_style.scss';

const CellRenderer = ({ title, value, componentName }) => {
  const { t } = useTranslation();
  if (title === 'admissionDate') console.log(`${title}: ${value}`);
  return (
    <div className="CellRenderer">
      <p className="TableLabel">{t(`${componentName}.table.columns.${title}`)}</p>
      <p className="TableText">{value}</p>
    </div>
  );
};

CellRenderer.propTypes = {
  title: PropTypes.string.isRequired,
  value: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
  componentName: PropTypes.string.isRequired
};

export default CellRenderer;
