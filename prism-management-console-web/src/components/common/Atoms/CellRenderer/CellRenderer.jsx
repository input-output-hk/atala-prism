import React from 'react';
import PropTypes from 'prop-types';
import { childrenType } from '../../../../helpers/propShapes';
import './_style.scss';

const CellRenderer = ({ title, value, firstValue, children, light }) => (
  <div className="CellRenderer">
    <p className="TableLabel">{title}</p>
    <p className={`TableText ${light ? 'light' : ''}`}>{value}</p>
    {children}
    <p className="TableTextBold">{firstValue}</p>
  </div>
);

CellRenderer.defaultProps = {
  children: null,
  value: null,
  light: false,
  firstValue: ''
};

CellRenderer.propTypes = {
  title: PropTypes.string.isRequired,
  value: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  firstValue: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  children: childrenType,
  light: PropTypes.bool
};

export default CellRenderer;
