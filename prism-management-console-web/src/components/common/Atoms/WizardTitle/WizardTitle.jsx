import React from 'react';
import PropTypes from 'prop-types';
import './_style.scss';

const WizardTitle = ({ title, subtitle }) => (
  <div className="WizardTitle">
    <div className="TypeSelectionContainer">
      <div className="titleContainer">
        <h1>{title}</h1>
        <h3>{subtitle}</h3>
      </div>
    </div>
  </div>
);

WizardTitle.propTypes = {
  title: PropTypes.string.isRequired,
  subtitle: PropTypes.string.isRequired
};

export default WizardTitle;
