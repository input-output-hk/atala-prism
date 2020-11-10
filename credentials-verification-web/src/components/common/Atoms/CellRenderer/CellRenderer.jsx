import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';

import './_style.scss';

const CellRenderer = ({ title, value, firstValue, componentName, children }) => {
  const { t } = useTranslation();
  return (
    <div className="CellRenderer">
      <p className="TableLabel">{t(`${componentName}.table.columns.${title}`)}</p>
      <p className="TableText">{value}</p>
      {children}
      <p className="TableTextBold">{firstValue}</p>
    </div>
  );
};

CellRenderer.defaultProps = {
  children: null
};

CellRenderer.propTypes = {
  title: PropTypes.string.isRequired,
  value: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
  firstValue: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
  componentName: PropTypes.string.isRequired,
  children: PropTypes.arrayOf(PropTypes.element)
};

export default CellRenderer;
