import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';

import './_style.scss';

const CellRenderer = ({ title, value, firstValue, children, light }) => {
  const { t } = useTranslation();
  return (
    <div className="CellRenderer">
      <p className="TableLabel">{title}</p>
      <p className={`TableText ${light ? 'light' : ''}`}>{value}</p>
      {children}
      <p className="TableTextBold">{firstValue}</p>
    </div>
  );
};

CellRenderer.defaultProps = {
  children: null,
  light: false
};

CellRenderer.propTypes = {
  title: PropTypes.string.isRequired,
  value: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
  firstValue: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
  children: PropTypes.arrayOf(PropTypes.element),
  light: PropTypes.bool
};

export default CellRenderer;
