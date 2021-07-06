import React from 'react';
import PropTypes from 'prop-types';

import './_style.scss';

const IntroItem = ({ itemIcon, itemTitle, itemText }) => (
  <div className="IntroItem">
    <div className="HeaderItem">
      <img src={itemIcon} alt="Item Icon" />
      <h2>{itemTitle}</h2>
      <p>{itemText}</p>
    </div>
  </div>
);

IntroItem.propTypes = {
  itemIcon: PropTypes.string.isRequired,
  itemTitle: PropTypes.string.isRequired,
  itemText: PropTypes.string.isRequired,
  bulletText1: PropTypes.string.isRequired,
  bulletText2: PropTypes.string.isRequired,
  bulletText3: PropTypes.string.isRequired,
  bulletText4: PropTypes.string.isRequired,
  bulletText5: PropTypes.string.isRequired
};

export default IntroItem;
