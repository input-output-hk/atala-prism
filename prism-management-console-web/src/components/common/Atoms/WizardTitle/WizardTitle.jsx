import React from 'react';

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

export default WizardTitle;
