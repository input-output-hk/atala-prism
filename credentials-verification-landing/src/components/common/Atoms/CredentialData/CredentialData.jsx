import React from 'react';
import PropTypes from 'prop-types';
import CellRenderer from '../CellRenderer/CellRenderer';
import './_style.scss';

const CredentialData = ({
  credentialHeaderClassName,
  credentialTemplateClassName,
  credentialContentClassName,
  iconLeft,
  iconRight,
  iconAlt,
  componentName,
  cellTitle,
  cellList
}) => {
  const CellsRenderer = cells =>
    cells.map(cell =>
      cell.subList ? (
        <div className={cell.className}>{CellsRenderer(cell.subList)}</div>
      ) : (
        <>
          <CellRenderer key={cell.title} componentName={componentName} {...cell} />
          <hr />
        </>
      )
    );
  return (
    <div className={credentialTemplateClassName}>
      <div className={credentialHeaderClassName}>
        {iconLeft && <img className="IconUniversity" src={iconLeft} alt={iconAlt} />}
        <CellRenderer componentName={componentName} {...cellTitle} />
        {iconRight && <img className="IconUniversity" src={iconRight} alt={iconAlt} />}
      </div>
      <div className={credentialContentClassName}>{CellsRenderer(cellList)}</div>
    </div>
  );
};

CredentialData.defaultProps = {
  credentialHeaderClassName: 'CredentialHeader',
  credentialTemplateClassName: 'CredentialTemplate',
  credentialContentClassName: 'CredentialContent',
  iconLeft: undefined,
  iconRight: undefined
};

CredentialData.propTypes = {
  credentialHeaderClassName: PropTypes.string,
  credentialTemplateClassName: PropTypes.string,
  credentialContentClassName: PropTypes.string,
  iconLeft: PropTypes.string,
  iconRight: PropTypes.string,
  iconAlt: PropTypes.string.isRequired,
  componentName: PropTypes.string.isRequired,
  cellTitle: PropTypes.shape({ title: PropTypes.string, value: PropTypes.any }).isRequired,
  cellList: PropTypes.arrayOf(PropTypes.shape({ title: PropTypes.string, value: PropTypes.any })).isRequired
};

export default CredentialData;
